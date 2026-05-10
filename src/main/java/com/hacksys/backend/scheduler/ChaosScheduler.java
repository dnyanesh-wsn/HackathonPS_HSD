package com.hacksys.backend.scheduler;

import com.hacksys.backend.model.Order;
import com.hacksys.backend.service.InventoryService;
import com.hacksys.backend.service.OrderService;
import com.hacksys.backend.service.PaymentService;
import com.hacksys.backend.util.LogStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

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
                } catch (RuntimeException e) {
                    String[] eCodes = {"PAYMENT_PROCESSING_ERROR", "RECON_PAY_FAIL", "PAY_CYCLE_ERR"};
                    logStore.error(SVC, traceId, eCodes[random.nextInt(eCodes.length)],
                        "payment processing failed orderId=" + order.getId() + " msg=" + e.getMessage());
                }
            }

            if (random.nextDouble() < 0.2) {
                try {
                    orderService.cancelOrder(order.getId(), traceId);
                    logStore.warn(SVC, traceId, "ORDER_CANCELLED_EARLY",
                        "order voided during reconciliation window orderId=" + order.getId());
                } catch (Exception e) {
                    logStore.error(SVC, traceId, "CANCEL_ERROR",
                        "cancellation error during reconciliation orderId=" + order.getId());
                }
            }
        } catch (RuntimeException e) {
            logStore.error(SVC, traceId, "RECONCILIATION_CYCLE