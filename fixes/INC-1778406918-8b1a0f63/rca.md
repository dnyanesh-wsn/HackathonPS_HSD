# RCA — INC-1778406918-8b1a0f63

**Trace ID:** metrics-b1c088c6
**Service:** BackgroundWorker
**Confidence:** 52%
**Blast Radius:** LOW

## Root Cause
ChaosScheduler in BackgroundWorker failed to refresh the cache and poll feature flags within acceptable time windows because of elevated thread pool utilization (6/8 threads busy), causing downstream delays in configuration and cache propagation.

## Suggested Fix
In ChaosScheduler, reduce the number of concurrently scheduled tasks per cycle or increase the thread pool size. Add a task-priority queue so cache refresh and feature flag polling are scheduled on dedicated threads. Example config: `scheduler.thread-pool.size=12` or annotate critical refresh tasks with `@Scheduled(fixedDelay=..., executor="priorityExecutor")`.
