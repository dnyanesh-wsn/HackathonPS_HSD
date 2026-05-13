# RCA — INC-1778658944-77d7d93e

**Trace ID:** stock-sync-444e4a66
**Service:** BackgroundWorker
**Confidence:** 88%
**Blast Radius:** MEDIUM

## Root Cause
ChaosScheduler in BackgroundWorker failed to validate the warehouse delta before applying it because it initiated a hard stock deduction of qty=999 for PROD-005 without checking whether the current inventory level could absorb the deduction, directly causing an INV_COUNTER_UNDERFLOW and resulting in a nonsensical stock level of -3988.

## Suggested Fix
In ChaosScheduler.applyWarehouseDelta() (BackgroundWorker), add a pre-deduction guard that validates the delta against current stock before calling InventoryService. Additionally, in InventoryService.deductStock(), enforce a hard floor at zero and throw an exception on underflow rather than allowing negative commits. Example guard: `if (currentStock - delta < 0) throw new StockUnderflowException("Delta would underflow stock for " + productId);`
