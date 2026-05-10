# RCA — INC-1778416309-38a767e1

**Trace ID:** cascade-1c33861b
**Service:** BackgroundWorker
**Confidence:** 87%
**Blast Radius:** HIGH

## Root Cause
ChaosScheduler (BackgroundWorker) contains a flawed state-machine that incorrectly flagged the inventory reservation as incomplete (INV_PHASE_INCOMPLETE) despite OrderService confirming full reservation (status=RESERVED) ~4 seconds earlier. The scheduler then proceeded to retry the reservation and subsequently advanced to the payment phase despite an unresolved INV_RESERVATION_INCOMPLETE warning, resulting in a payment attempt on an order with uncertain inventory state. The payment phase then encountered an upstream gateway timeout (PAYMENT_SVC_TIMEOUT), leaving the order in a dangling state with stock held but payment unconfirmed.

## Suggested Fix
1. Fix ChaosScheduler state-machine to correctly consume and trust the OrderService RESERVED status event before evaluating inventory phase completion — it must not re-evaluate inventory phase if OrderService has already emitted status=RESERVED. 2. Add a hard gate in the scheduler: payment phase must NOT proceed if INV_RESERVATION_INCOMPLETE is active. 3. Implement idempotency guards on the reserve operation to prevent duplicate stock holds on retry. 4. Add circuit-breaker / exponential backoff on payment gateway retries with a dead-letter queue for failed payment attempts.
