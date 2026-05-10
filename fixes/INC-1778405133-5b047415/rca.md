# RCA — INC-1778405133-5b047415

**Trace ID:** stock-sync-cc1d8d92
**Service:** BackgroundWorker
**Confidence:** 88%
**Blast Radius:** HIGH

## Root Cause
ChaosScheduler in BackgroundWorker failed to validate the warehouse feed delta before applying it because it initiated a hard stock deduction of qty=999 for PROD-005 without checking whether the resulting stock level would fall below zero, driving inventory to -5985.

## Suggested Fix
In ChaosScheduler.applyWarehouseFeedDelta() and InventoryService.deductStock(), add a pre-deduction guard that rejects any operation resulting in negative stock. Example: `if (currentStock - deductionQty < 0) throw new InvalidStockOperationException("Deduction would result in negative stock for " + productId);`. Additionally, ChaosScheduler should validate the delta magnitude against a configurable threshold (e.g., `warehouse.sync.max-delta-qty=500`) before dispatching to InventoryService.
