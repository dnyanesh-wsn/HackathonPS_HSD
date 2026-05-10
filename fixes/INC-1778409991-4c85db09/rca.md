# RCA — INC-1778409991-4c85db09

**Trace ID:** blind-e8a19e35
**Service:** BackgroundWorker
**Confidence:** 88%
**Blast Radius:** HIGH

## Root Cause
InventoryService in InventoryService failed to complete the stock reservation for PROD-002 because the reservation operation timed out (INV_SVC_TIMEOUT), causing the order to be marked FAILED before the ChaosScheduler's background settlement worker independently routed and completed a payment against that already-failed order.

## Suggested Fix
ChaosScheduler.processSettlement() in BackgroundWorker must validate order status before routing payment. Add a guard that aborts settlement for any order not in a terminal-success state (e.g., CONFIRMED or RESERVED): `if (!order.getStatus().equals(OrderStatus.CONFIRMED)) { log.warn("Skipping settlement for non-confirmed order {}", orderId); return; }`. Additionally, PaymentService.initiatePayment() must enforce a hard pre-condition check on order status and throw an exception (not just a WARN) when status=FAILED, preventing payment persistence.
