# RCA — INC-1778661101-8b7c2583

**Trace ID:** pay-poll-a2153e8d
**Service:** BackgroundWorker
**Confidence:** 93%
**Blast Radius:** HIGH

## Root Cause
InventoryService reservation timed out (INV_SVC_TIMEOUT) for PROD-002, causing the order af082e01-5633-4f89-a98b-0f8c94ddb22f to be marked FAILED/CANCELLED. However, the BackgroundWorker payment poller — operating asynchronously and without order-state validation — proceeded to initiate and complete payment processing against the already-cancelled order. PaymentService accepted the auth despite detecting UNEXPECTED_ORDER_STATUS, resulting in a PAID_AFTER_CANCEL state: customer was charged for a failed, undeliverable order with no stock reserved and no inventory committed.

## Suggested Fix
1) PaymentService must validate order state before accepting or processing any payment authorization — orders in FAILED, CANCELLED, or any terminal state must be hard-rejected, not warned and continued. 2) The BackgroundWorker payment poller must perform an authoritative order-state check (not just a warning) before dispatching payments. 3) Implement a distributed saga/compensating transaction: if reservation fails, the payment step must never be enqueued or must be rolled back atomically. 4) Fix the stock release path on cancellation to ensure reservations are always voided even when the initial reservation partially failed.
