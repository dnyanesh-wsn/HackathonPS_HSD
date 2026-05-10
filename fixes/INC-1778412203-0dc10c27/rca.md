# RCA — INC-1778412203-0dc10c27

**Trace ID:** stock-sync-30fea9a1
**Service:** BackgroundWorker
**Confidence:** 88%
**Blast Radius:** HIGH

## Root Cause
ChaosScheduler in BackgroundWorker failed to validate the warehouse feed delta before applying it because it initiated a hard stock deduction of qty=999 for PROD-005 without checking whether sufficient stock existed, driving the inventory counter to -2989.

## Suggested Fix
In ChaosScheduler.applyWarehouseDelta() (BackgroundWorker), add a pre-deduction guard that aborts and raises an alert if the resulting stock would fall below zero. In InventoryService, enforce a non-negative constraint at the persistence layer: e.g., `if (currentStock - qty < 0) throw new InsufficientStockException(productId, currentStock, qty);`
