# RCA — INC-1778653991-1c2502e6

**Trace ID:** cascade-102907c9
**Service:** BackgroundWorker
**Confidence:** 92%
**Blast Radius:** HIGH

## Root Cause
InventoryService experienced repeated reservation timeouts (INV_SVC_TIMEOUT) for both PROD-004 and PROD-001, causing STOCK_HOLD_FAILED errors and marking the order as FAILED (PARTIAL_RESERVATION). The ChaosScheduler background worker then proceeded to the payment phase despite the inventory reservation being incomplete (INV_RESERVATION_INCOMPLETE), bypassing the order failure state. PaymentService detected the state mismatch (ORDER_STATE_MISMATCH) but still accepted and persisted the payment, resulting in a paid order with no confirmed stock reservation.

## Suggested Fix
The ChaosScheduler must enforce a hard gate: if the inventory phase does not complete successfully, the pipeline must not advance to payment. The PaymentService must reject payment attempts for orders not in a valid payable state (e.g., RESERVED) rather than logging a warning and proceeding. Additionally, InventoryService timeout thresholds and retry logic should be reviewed to prevent cascading reservation failures.
