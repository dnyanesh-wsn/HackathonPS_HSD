# RCA — INC-1778661101-462569a9

**Trace ID:** stock-sync-c6f3d95f
**Service:** BackgroundWorker
**Confidence:** 85%
**Blast Radius:** HIGH

## Root Cause
ChaosScheduler in BackgroundWorker failed to validate the warehouse delta feed before applying it because it blindly initiated a hard stock deduction of qty=999 for PROD-005 without checking whether the resulting stock level would fall below zero, driving inventory to -10976.

## Suggested Fix
In ChaosScheduler.applyWarehouseDelta() (BackgroundWorker), add a pre-flight validation step before invoking the deduction: if (currentStock - delta < 0) { log.error('Delta rejected: would result in negative stock'); return; }. Additionally, in InventoryService.deductStock(), enforce a hard floor guard: if (newStock < 0) throw new InvalidStockOperationException('Stock cannot go negative'); to prevent persistence of invalid state.
