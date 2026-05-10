# RCA — INC-1778404897-497de33e

**Trace ID:** blind-cb01080c
**Service:** BackgroundWorker
**Confidence:** 88%
**Blast Radius:** HIGH

## Root Cause
InventoryService in InventoryService failed to complete the stock reservation for PROD-002 because the reservation operation timed out (INV_SVC_TIMEOUT), causing the order to be marked FAILED before payment processing began.

## Suggested Fix
In PaymentService.initiatePayment() (or equivalent auth method), enforce a hard guard that aborts payment processing when ORDER_STATE_MISMATCH is detected for terminal states (FAILED, CANCELLED). Example: `if (order.getStatus() == OrderStatus.FAILED) { throw new InvalidOrderStateException("Cannot process payment for FAILED order: " + orderId); }`. Additionally, ChaosScheduler.scheduleSettlement() must query current order status before routing and skip non-payable orders: add config key `settlement.worker.skip-terminal-states=FAILED,CANCELLED`.
