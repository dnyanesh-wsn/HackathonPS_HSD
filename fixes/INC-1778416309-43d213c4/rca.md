# RCA — INC-1778416309-43d213c4

**Trace ID:** stock-sync-fbf5082e
**Service:** BackgroundWorker
**Confidence:** 88%
**Blast Radius:** HIGH

## Root Cause
InventoryService in InventoryService failed to validate stock levels before applying a hard deduction because it processed a delta of qty=999 against PROD-005 without checking for sufficient inventory, resulting in a counter underflow and a final stock level of -2989.

## Suggested Fix
In InventoryService, add a pre-deduction guard in the hard deduction method (e.g., applyHardDeduction()) to reject or clamp operations that would result in negative stock: `if (currentStock - qty < 0) throw new InsufficientStockException(productId, currentStock, qty);`. Additionally, ChaosScheduler should roll back or dead-letter the feed delta when STOCK_LEVEL_ANOMALY is detected rather than marking sync complete.
