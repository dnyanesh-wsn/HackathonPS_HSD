package com.hacksys.backend.scheduler;

import com.hacksys.backend.model.Order;
import com.hacksys.backend.service.InventoryService;
import com.hacksys.backend.service.OrderService;
import com.hacksys.backend.service.PaymentService;
import com.hacksys.backend.util.LogStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Component
public class ChaosScheduler {

    private static final Logger log = LoggerFactory.getLogger(ChaosScheduler.class);
    private static final String SVC = "BackgroundWorker";

    private final LogStore logStore;
    private final OrderService orderService;
    private final PaymentService paymentService;
    private final InventoryService inventoryService;
    private final Random random = new Random();

    private final ConcurrentHashMap<String, Long> pendingReconciliation = new ConcurrentHashMap<>();

    // Dedicated single-thread executor for cache refresh so it is never starved by the shared pool
    private final Executor cacheRefreshExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "cache-refresh-dedicated");
        t.setDaemon(true);
        return t;
    }); // WHY: isolates cache-refresh work from the shared scheduler pool that was reaching 6/8 utilization

    // Dedicated single-thread executor for feature-flag polling to prevent timeout under thread contention
    private final Executor featureFlagExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "feature-flag-dedicated");
        t.setDaemon(true);
        return t;
    }); // WHY: feature-flag poll timed out and fell back to stale config because shared threads were saturated

    private static final String[] USER_IDS = {
        "user-101", "user-202", "user-303", "user-404", "user-505"
    };
    private static final String[] PRODUCT_IDS = {
        "PROD-001", "PROD-002", "PROD-003", "PROD-004", "PROD-005"
    };

    public ChaosScheduler(LogStore logStore, OrderService orderService,
                          PaymentService paymentService, InventoryService inventoryService) {
        this.logStore = logStore;
        this.orderService = orderService;
        this.paymentService = paymentService;
        this.inventoryService = inventoryService;
    }

    // Reconciliation worker — processes pending orders and retries failed transitions
    @Scheduled(fixedDelay = 30000, initialDelay = 5000)
    public void reconciliationWorker() {
        String traceId = "recon-" + UUID.randomUUID().toString().substring(0, 8);
        String userId = USER_IDS[random.nextInt(USER_IDS.length)];
        String productId = PRODUCT_IDS[random.nextInt(PRODUCT_IDS.length)];
        int quantity = 1 + random.nextInt(5);

        logStore.info(SVC, traceId, "reconciliation cycle start userId=" + userId);
        try {
            List<Order.OrderItem> items = List.of(new Order.OrderItem(productId, quantity, 79.99));
            Order order = orderService.createOrder(userId, items, traceId);
            pendingReconciliation.put(order.getId(), System.currentTimeMillis());

            if (random.nextDouble() < 0.3) {
                try {
                    paymentService.processPayment(order.getId(), userId, quantity * 79.99, traceId);