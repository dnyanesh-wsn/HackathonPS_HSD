# RCA — INC-1778659703-db58d1ba

**Trace ID:** ASYNC-ORPHAN
**Service:** PaymentService
**Confidence:** 87%
**Blast Radius:** HIGH

## Root Cause
PaymentService async confirmation pipeline is experiencing systemic timeouts. The confirmation service is failing to acknowledge payment completions, resulting in repeated PAY_CONFIRM_ERR and CONFIRM_NOTIFICATION_FAILED errors across multiple paymentIds over a ~13 minute window. The downstream notification dispatcher is either unavailable, overloaded, or has a broken async consumer causing ack timeouts.

## Suggested Fix
1. Investigate the confirmation service health and message queue consumer lag for the async ack channel. 2. Check for thread pool exhaustion or connection pool saturation in the PaymentService async executor. 3. Verify the notification dispatcher endpoint is reachable and not rate-limited. 4. Implement a dead-letter queue (DLQ) for failed confirmation events to prevent silent loss and enable replay.
