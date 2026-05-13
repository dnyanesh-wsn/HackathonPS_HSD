# RCA — INC-1778658196-9b696fb1

**Trace ID:** blind-1190cd6d
**Service:** BackgroundWorker
**Confidence:** 92%
**Blast Radius:** HIGH

## Root Cause
InventoryService reservation timed out (INV_SVC_TIMEOUT) for PROD-002, causing the order f825638a-6fbb-47a7-b629-d8271315f5f0 to be marked FAILED via PARTIAL_RESERVATION. Despite the order being in a FAILED state, the BackgroundWorker ChaosScheduler incorrectly routed a payment settlement for the same order. PaymentService acknowledged an ORDER_STATE_MISMATCH warning but still processed and persisted the payment (paymentId=affbdf94-856c-4f03-9297-f45b79bb1d1d, amount=129.99), resulting in a payment being charged against a failed, unfulfilled order. Additionally, a DB_WRITE_FAILURE prevented the order status from being updated post-payment, leaving the system in an inconsistent state.

## Suggested Fix
1. Implement a strict order-state guard in PaymentService that hard-rejects payment initiation for any order not in a payable state (e.g., CONFIRMED/PENDING_PAYMENT), rather than emitting a warning and proceeding. 2. Fix ChaosScheduler settlement routing to validate order status before dispatching payment — FAILED orders must be excluded from settlement queues. 3. Resolve the DB_WRITE_FAILURE root cause (likely DB connectivity or lock contention) to ensure order state transitions are durably persisted. 4. Initiate an immediate refund for paymentId=affbdf94-856c-4f03-9297-f45b79bb1d1d.
