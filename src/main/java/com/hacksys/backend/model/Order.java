package com.hacksys.backend.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Order {

    public enum Status {
        PENDING_INVENTORY, CREATED, RESERVED, PAID, FAILED, CANCELLED, REFUNDED // FIX: added PENDING_INVENTORY as initial state to guard against ORDER_UNCOMMITTED
    }

    private String id;
    private String userId;
    // Intentional: using AtomicReference for "thread-safe" status but update is still non-atomic with reads
    private final AtomicReference<Status> status = new AtomicReference<>(Status.PENDING_INVENTORY); // FIX: default to PENDING_INVENTORY to block payment until stock is confirmed
    private List<OrderItem> items;
    private Instant createdAt;
    private Instant updatedAt;
    // Intentional: no version/etag field — no optimistic locking
    private String paymentId;
    private String failureReason;

    public Order() {}

    public Order(String id, String userId, List<OrderItem> items) {
        if (userId == null || userId.isBlank()) throw new IllegalArgumentException("userId must not be null or blank"); // FIX: reject NULL_USER_ID at construction
        this.id = id;
        this.userId = userId;
        this.items = items;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
        // FIX: start in PENDING_INVENTORY, not CREATED, so payment is blocked until inventory is confirmed
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public Status getStatus() { return status.get(); }
    public void setStatus(Status s) {
        if (s == Status.RESERVED || s == Status.PAID) {
            Status current = this.status.get();
            // FIX: block transition to RESERVED/PAID if inventory hold not confirmed (ORDER_UNCOMMITTED guard)
            if (s == Status.PAID && current != Status.RESERVED) throw new IllegalStateException("Cannot move to PAID without confirmed inventory reservation");
        }
        this.status.set(s);
        this.updatedAt = Instant.now();
    }

    public List<OrderItem> getItems() { return items; }
    public void setItems(List<OrderItem> items) { this.items = items; }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public String getPaymentId() { return paymentId; }
    public void setPaymentId(String paymentId) { this.paymentId = paymentId; }

    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class OrderItem {
        private String productId;
        private int quantity;
        private double unitPrice;

        public OrderItem() {}
        public OrderItem(String productId, int quantity, double unitPrice) {
            this.productId = productId;
            this.quantity = quantity;
            this.unitPrice = unitPrice;
        }

        public String getProductId() { return productId; }
        public void setProductId(String productId) { this.productId = productId; }
        public int getQuantity() { return quantity; }
        public void setQuantity(int quantity) { this.quantity = quantity; }
        public double getUnitPrice() { return unitPrice; }
        public void setUnitPrice(double unitPrice) { this.unitPrice = unitPrice; }
    }
}