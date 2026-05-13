# RCA — INC-1778661101-abcb8ffd

**Trace ID:** pay-retry-5476ed86
**Service:** BackgroundWorker
**Confidence:** 95%
**Blast Radius:** HIGH

## Root Cause
InventoryService reservation timeout (INV_SVC_TIMEOUT) caused STOCK_HOLD_FAILED, marking order d16876b3-417d-45b1-a79b-ba5f925b1906 as FAILED before payment was attempted. The ChaosScheduler payment retry worker then initiated payment authorization against the already-FAILED order, bypassing order state validation guards. The retry worker triggered two sequential payment authorizations without idempotency checks, resulting in two successful payment records (paymentId=0081cb2a and paymentId=85712ec2) being persisted for the same order — a duplicate charge of $79.98 to the customer.

## Suggested Fix
1. Immediately refund duplicate payment paymentId=85712ec2-5f58-46de-9682-206fd39d9917 for orderId=d16876b3-417d-45b1-a79b-ba5f925b1906. 2. Add strict order-state pre-checks in PaymentService that hard-reject (not warn) payment initiation when order status is FAILED or any terminal state. 3. Implement idempotency keys in the retry worker so that a single retry sweep cannot produce more than one payment authorization per orderId. 4. Fix ChaosScheduler to validate order eligibility (status=CREATED or PENDING_PAYMENT only) before enqueuing retry attempts. 5. Resolve inventory reservation reliability to prevent the upstream FAILED state that triggered the retry path.
