# RCA — INC-1778658053-755eeede

**Trace ID:** stock-sync-5175c301
**Service:** BackgroundWorker
**Confidence:** 88%
**Blast Radius:** MEDIUM

## Root Cause
ChaosScheduler in BackgroundWorker failed to validate the warehouse feed delta before applying it because it initiated a hard stock deduction of qty=999 for PROD-005 without checking whether sufficient inventory existed, causing an inventory counter underflow and resulting in a negative stock level of -991.

## Suggested Fix
In ChaosScheduler.applyWarehouseFeedDelta(), add a pre-flight check that compares the incoming delta against the current stock level before invoking InventoryService.hardDeduct(). Additionally, add a floor guard in InventoryService.hardDeduct(): `if (currentStock - qty < 0) throw new InventoryUnderflowException(productId, currentStock, qty);` to prevent the deduction from being committed when it would produce a negative result.
