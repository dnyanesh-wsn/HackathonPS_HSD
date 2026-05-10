# RCA — INC-1778415915-7ca24aa6

**Trace ID:** cascade-f61b25ad
**Service:** BackgroundWorker
**Confidence:** 91%
**Blast Radius:** HIGH

## Root Cause
InventoryService timed out during stock reservation for PROD-004 (INV_TIMEOUT), causing STOCK_HOLD_FAILED in OrderService. The reservation phase completed only partially — PROD-001 was reserved (qty=1, remaining=44) but PROD-004 was not. Despite the incomplete inventory reservation, the ChaosScheduler retry also failed to resolve the reservation and proceeded to the payment phase anyway (INV_RESERVATION_INCOMPLETE bypassed), resulting in payment of $179.97 being captured for an order with an unconfirmed/partial stock hold. The order remains in an inconsistent state: status=PAID but inventory reservation is incomplete.

## Suggested Fix
The ChaosScheduler must not advance to the payment phase when inventory reservation is incomplete. Implement a hard gate: payment initiation must be blocked unless all line-item reservations are confirmed. Add idempotent compensation logic to release the PROD-001 reservation if PROD-004 reservation cannot be fulfilled after max retries. For the current incident, manually verify PROD-004 stock state, issue a refund for paymentId=2fa1ceff-b6f3-4a33-8fa8-171e442d0ef8 or complete the reservation, and reconcile the order status.
