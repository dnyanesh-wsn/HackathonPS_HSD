# RCA — INC-1778660641-67d077f7

**Trace ID:** stock-sync-48b565d3
**Service:** BackgroundWorker
**Confidence:** 88%
**Blast Radius:** MEDIUM

## Root Cause
ChaosScheduler in BackgroundWorker failed to validate the warehouse delta before applying it because it blindly initiated a hard stock deduction of qty=999 for PROD-005 without checking whether sufficient stock existed, driving the counter to -991.

## Suggested Fix
In ChaosScheduler.applyWarehouseDelta() (BackgroundWorker), add a pre-deduction guard that rejects any delta that would result in negative stock, and in InventoryService.deductStock(), enforce a hard floor: `if (currentStock - qty < 0) throw new InsufficientStockException("Deduction would underflow: " + productId);`
