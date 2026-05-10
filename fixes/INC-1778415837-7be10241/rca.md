# RCA — INC-1778415837-7be10241

**Trace ID:** stock-sync-1cbfb8c7
**Service:** BackgroundWorker
**Confidence:** 88%
**Blast Radius:** HIGH

## Root Cause
ChaosScheduler in BackgroundWorker failed to validate the warehouse feed delta before applying it because it initiated a hard stock deduction of qty=999 against PROD-005 without checking whether sufficient stock existed, driving the inventory counter to -991.

## Suggested Fix
In ChaosScheduler.applyWarehouseFeedDelta() (BackgroundWorker), add a pre-deduction guard that rejects any delta that would result in stock < 0, and in InventoryService enforce a floor constraint on deduction: `if (currentStock - qty < 0) throw new InsufficientStockException(productId, currentStock, qty);`
