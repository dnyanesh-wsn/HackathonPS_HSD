# RCA — INC-1778404850-ed546cf8

**Trace ID:** metrics-c62f0dc6
**Service:** BackgroundWorker
**Confidence:** 62%
**Blast Radius:** MEDIUM

## Root Cause
ChaosScheduler in BackgroundWorker is exhibiting early signs of resource pressure because the connection pool is near capacity (8/10 in use) and the metrics collector buffer is lagging at 30ms, indicating the scheduler is approaching saturation under its current workload cycle.

## Suggested Fix
In ChaosScheduler, increase the connection pool ceiling and introduce task-level throttling. Raise pool max-size via config key `db.pool.maxSize=20` and add a semaphore guard in the task dispatch loop: `Semaphore taskSlots = new Semaphore(8);` to prevent all pool connections from being consumed in a single cycle.
