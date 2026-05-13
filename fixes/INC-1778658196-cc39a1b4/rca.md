# RCA — INC-1778658196-cc39a1b4

**Trace ID:** stock-sync-ac2ffbea
**Service:** BackgroundWorker
**Confidence:** 88%
**Blast Radius:** HIGH

## Root Cause
ChaosScheduler in BackgroundWorker failed to validate the warehouse delta before applying it because it blindly applied a deduction of qty=999 to PROD-005 without checking current stock levels, causing InventoryService to drive the stock counter to -1990.

## Suggested Fix
In ChaosScheduler.applyWarehouseDelta(), add a pre-deduction guard that reads the current stock level and rejects or quarantines any delta that would result in a negative value. Example: `if (currentStock - delta < 0) { quarantineDelta(productId, delta); return; }`. Additionally, InventoryService.deductStock() should enforce a floor constraint (minimum stock = 0) and throw an INV_COUNTER_UNDERFLOW exception rather than silently completing.
