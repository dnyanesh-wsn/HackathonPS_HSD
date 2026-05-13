# RCA — INC-1778661101-337d0556

**Trace ID:** stock-sync-ee0d4de3
**Service:** BackgroundWorker
**Confidence:** 88%
**Blast Radius:** HIGH

## Root Cause
ChaosScheduler in BackgroundWorker failed to validate the warehouse feed delta before applying it because it initiated a hard stock deduction of qty=999 against PROD-005 without checking whether the resulting stock level would fall below zero, driving the counter to -5986.

## Suggested Fix
In ChaosScheduler.applyWarehouseFeedDelta() (BackgroundWorker), add a pre-flight guard before invoking the deduction: if (currentStock - delta < 0) { rejectDelta(productId, delta); return; }. Additionally, InventoryService.deductStock() should enforce a floor constraint and throw an InsufficientStockException rather than persisting a negative value.
