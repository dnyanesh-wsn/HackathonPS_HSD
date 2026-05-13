# RCA — INC-1778669053-eec12644

**Trace ID:** audit-b4663247
**Service:** BackgroundWorker
**Confidence:** 52%
**Blast Radius:** LOW

## Root Cause
ChaosScheduler in BackgroundWorker detected that orderId=a0f3deb3-4f35-4d71-b4c4-62e8a16e2527 failed to transition out of CREATED state within SLA because the order remained uncommitted beyond the expected commitment window, indicating a likely upstream failure in order confirmation or inventory hold application.

## Suggested Fix
In ChaosScheduler.scanStaleOrders(), upon detecting a stale CREATED order, trigger an automated compensating action: either re-attempt the inventory hold via InventoryService.applyHold(orderId) or cancel and release the order. Add a config key `stale.order.sla.seconds` to tune the SLA threshold and a dead-letter queue entry for manual review. Example: `if (order.getAge() > staleOrderSlaSeconds) { compensationService.cancelOrRetryHold(order.getId()); }`
