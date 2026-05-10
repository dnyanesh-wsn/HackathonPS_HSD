package com.hacksys.backend.service;

import com.hacksys.backend.model.InventoryItem;
import com.hacksys.backend.model.Order;
import com.hacksys.backend.util.LogStore;
import com.hacksys.backend.util.TraceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;

/**
 * OrderService — manages order lifecycle including creation, reservation, payment and cancellation.
 */
@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);
    private static final String SVC = "OrderService";

    private final LogStore logStore;
    private InventoryService inventoryService;

    @Value("${app.chaos.intermittent-failure-rate:0.25}")
    private double failureRate;

    // Timeout in milliseconds for a single inventory reachability probe before persisting the order
    @Value("${app.inventory.reachability-timeout-ms:3000}")
    private long inventoryReachabilityTimeoutMs; // WHY: makes the timeout configurable rather than hard-coded

    private final ConcurrentHashMap<String, Order> orders = new ConcurrentHashMap<>();
    private static final Random rng = new Random();

    // Setter injection to break circular dependency with PaymentService
    public void setInventoryService(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    public OrderService(LogStore logStore) {
        this.logStore = logStore;
    }

    /**
     * Probe whether the inventory service is reachable and responsive within the configured timeout.
     * Returns true if reachable, false (or throws) if the probe times out or the service is null.
     */
    private boolean isInventoryServiceReachable(String traceId) { // WHY: surface timeout as synchronous failure before persisting the order
        if (inventoryService == null) {
            log.error("Inventory service is not wired — cannot validate reachability");
            logStore.error(SVC, traceId, "INV_SERVICE_UNAVAILABLE",
                    "Inventory service reference is null — order creation aborted");
            return false;
        }
        try {
            CompletableFuture<Boolean> probe = CompletableFuture.supplyAsync(
                    () -> inventoryService.isHealthy()); // WHY: run the probe on a separate thread so we can enforce a hard timeout
            probe.get(inventoryReachabilityTimeoutMs, TimeUnit.MILLISECONDS); // WHY: hard timeout converts a silent INV_HOLD_TIMEOUT into a synchronous exception
            return true;
        } catch (TimeoutException e) {
            log.error("Inventory service reachability probe timed out after {}ms — aborting order creation",
                    inventoryReachabilityTimeoutMs);
            logStore.error(SVC, traceId, "INV_HOLD_TIMEOUT", // WHY: use the canonical error code from the causal chain so downstream systems can correlate
                    "Inventory service did not respond within " + inventoryReachabilityTimeoutMs +
                    "ms — order creation aborted to prevent unresolved reservation state");
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // WHY: restore interrupt flag per best practice
            log.error("Inventory reachability probe interrupted — aborting order creation");
            logStore.error(SVC, traceId, "INV_PROBE_INTERRUPTED",
                    "Inventory probe interrupted — order creation aborted");
            return false;
        } catch (Exception e) {
            log.error("Inventory reachability probe failed with exception — aborting order creation error={}",
                    e.getMessage());
            logStore.error(SVC, traceId,