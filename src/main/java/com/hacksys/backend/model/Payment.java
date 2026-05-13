package com.hacksys.backend.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Payment {

    public enum Status {
        PENDING, SUCCESS, FAILED, REFUNDED, DUPLICATE
    }

    private String id;
    private String orderId;
    private String userId;
    private double amount;
    private Status status;
    private Instant createdAt;
    private Instant processedAt;
    private String failureReason;
    private String idempotencyKey; // WHY: added to prevent duplicate charges on retry by tracking unique payment attempts
    private int attemptCount;

    public Payment() {}

    public Payment(String id, String orderId, String userId, double amount) {
        this.id = id;
        this.orderId = orderId;
        this.userId = userId;
        this.amount = amount;
        this.status = Status.PENDING;
        this.createdAt = Instant.now();
        this.attemptCount = 1;
        this.idempotencyKey = orderId + ":" + id; // WHY: stable key scoped to order+payment prevents duplicate gateway charges on retry
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) {
        this.status = status;
        this.processedAt = Instant.now();
    }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getProcessedAt() { return processedAt; }

    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String reason) { this.failureReason = reason; }

    public int getAttemptCount() { return attemptCount; }
    public void setAttemptCount(int count) { this.attemptCount = count; }

    public String getIdempotencyKey() { return idempotencyKey; } // WHY: expose key so gateway client can send it on every attempt
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
}