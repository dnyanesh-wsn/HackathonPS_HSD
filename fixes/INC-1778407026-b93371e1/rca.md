# RCA — INC-1778407026-b93371e1

**Trace ID:** stock-sync-b2a3da75
**Service:** BackgroundWorker
**Confidence:** 88%
**Blast Radius:** HIGH

## Root Cause
ChaosScheduler in BackgroundWorker failed to validate the warehouse feed delta before applying it because it initiated a hard stock deduction of qty=999 against PROD-005 without checking available stock levels, driving inventory to -2989 and completing the commit despite the negative stock condition.

## Suggested Fix
In ChaosScheduler.applyWarehouseFeedDelta() and InventoryService.deductStock(), add a pre-deduction guard that aborts and rolls back if the resulting stock would go negative. Example: `if (currentStock - deductionQty < 0) { throw new InsufficientStockException("Deduction would result in negative stock for " + productId); }`. Additionally, InventoryService should never finalize a deduction when NEGATIVE_STOCK is detected — the commit path must check the post-deduction value before persisting.
