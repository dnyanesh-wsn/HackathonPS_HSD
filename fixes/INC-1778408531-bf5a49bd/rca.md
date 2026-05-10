# RCA — INC-1778408531-bf5a49bd

**Trace ID:** stock-sync-b8def4e3
**Service:** BackgroundWorker
**Confidence:** 88%
**Blast Radius:** HIGH

## Root Cause
InventoryService in InventoryService failed to enforce a non-negative stock floor during hard deduction because it applied a delta of qty=999 against insufficient on-hand inventory for PROD-005, allowing the counter to underflow to -991 without halting the operation.

## Suggested Fix
In InventoryService.applyHardDeduction() (or equivalent deduction method), add a pre-condition guard that throws an InsufficientStockException before committing if (currentStock - requestedQty) < 0. Example: `if (currentStock - qty < 0) throw new InsufficientStockException(productId, currentStock, qty);`. Additionally, ChaosScheduler.applyWarehouseDelta() should treat a STOCK_LEVEL_ANOMALY as a terminal error and invoke a rollback/compensating transaction rather than logging and continuing.
