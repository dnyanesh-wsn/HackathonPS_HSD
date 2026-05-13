# RCA — INC-1778658196-d3dc0da4

**Trace ID:** pay-poll-cb0da911
**Service:** BackgroundWorker
**Confidence:** 93%
**Blast Radius:** HIGH

## Root Cause
InventoryService timeout during stock reservation caused order 97a615c1-4765-4f04-aabc-a5cfa439ffc1 to be marked FAILED and cancelled. However, the BackgroundWorker payment poller (ChaosScheduler) had already queued a payment for this order and processed it after cancellation was complete, resulting in a payment of $129.99 being successfully persisted and applied to a FAILED/CANCELLED order. The payment service accepted the auth despite detecting UNEXPECTED_ORDER_STATUS, and OrderService subsequently updated the order to PAID — creating an inconsistent state where a cancelled order holds a committed payment with no corresponding stock reservation.

## Suggested Fix
The PaymentService must enforce a strict pre-authorization guard that rejects payment processing for any order not in a valid payable state (e.g., CONFIRMED or RESERVED). The UNEXPECTED_ORDER_STATUS warning must be treated as a hard abort, not a soft warning. Additionally, the BackgroundWorker payment poller must re-validate order status immediately before initiating payment, and cancelled/failed orders must be removed from or flagged in the payment queue atomically at cancellation time.
