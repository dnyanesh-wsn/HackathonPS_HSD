# RCA — INC-1778661101-1ccfce1f

**Trace ID:** blind-21a6ae78
**Service:** BackgroundWorker
**Confidence:** 88%
**Blast Radius:** HIGH

## Root Cause
InventoryService in InventoryService failed to complete the stock reservation for PROD-002 because of a transient store timeout, causing the order to be marked FAILED; subsequently, ChaosScheduler in BackgroundWorker routed a payment settlement for that already-FAILED order without checking its terminal status, resulting in AUTH_ON_TERMINAL_ORDER and an illegitimate PAID state.

## Suggested Fix
ChaosScheduler.processSettlement() in BackgroundWorker must validate order status before routing to PaymentService; add a guard: `if (!order.getStatus().equals(OrderStatus.CREATED)) { log.warn('Skipping settlement for terminal order {}', orderId); return; }`. Additionally, PaymentService.initiatePayment() should enforce a hard abort (throw IllegalStateException) rather than a soft warning when AUTH_ON_TERMINAL_ORDER is detected.
