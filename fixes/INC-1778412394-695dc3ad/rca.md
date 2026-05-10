# RCA — INC-1778412394-695dc3ad

**Trace ID:** stock-sync-e39ff544
**Service:** BackgroundWorker
**Confidence:** 88%
**Blast Radius:** HIGH

## Root Cause
ChaosScheduler in BackgroundWorker failed to validate the warehouse delta before applying it because it blindly applied a delta of qty=999 for PROD-005 without checking whether the deduction would underflow the current inventory counter, resulting in a committed stock level of -3988.

## Suggested Fix
In ChaosScheduler.applyWarehouseDelta() (BackgroundWorker), add a pre-deduction guard that compares the delta against current stock before invoking InventoryService, and abort with an alert if it would cause underflow. Additionally, in InventoryService.deductStock(), enforce a floor constraint: `if (newStock < 0) { throw new InventoryUnderflowException(productId, newStock); }` to prevent committing negative values.
