# RCA — INC-1778408689-8b5085bb

**Trace ID:** stock-sync-d8e8bd20
**Service:** BackgroundWorker
**Confidence:** 88%
**Blast Radius:** HIGH

## Root Cause
InventoryService in InventoryService failed to validate stock levels before applying a hard deduction because it allowed a qty=999 deduction against insufficient on-hand inventory, driving PROD-005 stock to -2989 without any pre-check or guard rail.

## Suggested Fix
In InventoryService.deductStock(), add a pre-deduction guard that checks current stock before applying the delta. Example: `if (currentStock - deductionQty < 0) { throw new InsufficientStockException(productId, currentStock, deductionQty); }`. ChaosScheduler.applyFeedDelta() should catch this exception and abort the commit rather than marking the sync complete.
