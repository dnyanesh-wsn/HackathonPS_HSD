# RCA — INC-1778660641-8445eb61

**Trace ID:** ASYNC-blind-335b1e74
**Service:** OrderService
**Confidence:** 52%
**Blast Radius:** MEDIUM

## Root Cause
OrderService in OrderService failed to transition order state after creation because the async pipeline stalled, leaving the order in a limbo state while PaymentService independently dispatched a payment confirmation — resulting in an untracked, potentially orphaned payment.

## Suggested Fix
In OrderService.processOrderPipeline() (or equivalent state machine method), implement a compensating transaction or dead-letter queue handler that detects pipeline stalls and either rolls back or emits a blocking signal to PaymentService before confirmation is dispatched. Example config: `order.pipeline.stall-timeout-ms=5000` paired with a circuit-breaker that prevents PaymentService from confirming payment if no ORDER_CONFIRMED event is received within the window.
