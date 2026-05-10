# RCA — INC-1778407768-e98c33cc

**Trace ID:** metrics-49a21b4d
**Service:** BackgroundWorker
**Confidence:** 62%
**Blast Radius:** MEDIUM

## Root Cause
ChaosScheduler in BackgroundWorker failed to maintain acceptable response times because heap memory pressure reached 78%, triggering increased GC activity that degraded throughput and caused downstream call timeouts, ultimately opening the circuit breaker.

## Suggested Fix
In ChaosScheduler, reduce heap pressure by tuning the JVM heap size and GC policy (e.g., set -XX:+UseG1GC -Xmx<appropriate_limit>) and audit for object retention/leaks in the scheduler loop. Additionally, add a backpressure mechanism in the downstream call path: ChaosScheduler.invokeDownstream() should implement exponential backoff before circuit open, e.g., `retryTemplate.setBackOffPolicy(new ExponentialBackOffPolicy())`. Connection pool idle reclamation (count=2 observed) should also be reviewed to ensure it is not prematurely releasing connections under GC pressure.
