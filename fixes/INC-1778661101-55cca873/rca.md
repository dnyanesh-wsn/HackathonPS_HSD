# RCA — INC-1778661101-55cca873

**Trace ID:** blind-1de19324
**Service:** BackgroundWorker
**Confidence:** 88%
**Blast Radius:** HIGH

## Root Cause
InventoryService in InventoryService failed to commit a stock reservation for PROD-002 because the warehouse feed was delayed, causing the stock hold to not be applied and the order to be marked FAILED. ChaosScheduler in BackgroundWorker then routed a payment settlement against this terminally-failed order, bypassing the order-state guard and resulting in a real payment being captured for a failed order.

## Suggested Fix
ChaosScheduler.routeSettlement() in BackgroundWorker must validate order status before initiating payment. Add a pre-flight guard: `if (!order.getStatus().equals(OrderStatus.CREATED) || order.getStatus().isTerminal()) { log.warn('Skipping settlement for terminal order {}', orderId); return; }`. Additionally, PaymentService.authorizePayment() must treat AUTH_ON_TERMINAL_ORDER as a hard abort, not a warning — replace the WARN log with a thrown exception or explicit rejection response.
