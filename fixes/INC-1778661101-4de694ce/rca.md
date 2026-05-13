# RCA — INC-1778661101-4de694ce

**Trace ID:** stock-sync-94c793b4
**Service:** BackgroundWorker
**Confidence:** 88%
**Blast Radius:** HIGH

## Root Cause
InventoryService in InventoryService failed to validate stock levels before applying a hard deduction because it blindly executed a delta of qty=999 against PROD-005 without checking whether sufficient stock existed, driving the balance to -9977.

## Suggested Fix
In InventoryService.deductStock() (or equivalent hard-deduction method), add a pre-deduction guard that rejects or clamps the operation if the resulting stock would fall below zero: `if (currentStock - qty < 0) throw new InsufficientStockException(productId, currentStock, qty);`. Additionally, ChaosScheduler.applyWarehouseDelta() should validate the post-sync value against a configured floor (e.g., `inventory.stock.min-allowed=0`) before committing.
