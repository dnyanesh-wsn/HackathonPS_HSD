# RCA — INC-1778668905-e5aabc20

**Trace ID:** stock-sync-ecd9b37e
**Service:** BackgroundWorker
**Confidence:** 88%
**Blast Radius:** HIGH

## Root Cause
ChaosScheduler in BackgroundWorker failed to validate the warehouse delta before applying it because it blindly issued a hard stock deduction of qty=999 for PROD-005 without checking whether sufficient stock existed, driving inventory to -991.

## Suggested Fix
In ChaosScheduler.applyWarehouseDelta() (BackgroundWorker), add a pre-deduction guard that fetches current stock and rejects or clamps the delta if it would result in negative stock. Example: `if (currentStock - delta < 0) { log.error('Delta rejected: would cause negative stock'); return; }`. Additionally, InventoryService.deductStock() should enforce a floor of zero or throw an InsufficientStockException rather than persisting negative values.
