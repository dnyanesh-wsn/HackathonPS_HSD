# RCA — INC-1778661101-61cbe1b2

**Trace ID:** stock-sync-33f8fdc2
**Service:** BackgroundWorker
**Confidence:** 85%
**Blast Radius:** HIGH

## Root Cause
ChaosScheduler in BackgroundWorker failed to validate the warehouse delta before applying it because it blindly applied a delta of qty=999 deduction against PROD-005 without checking whether the resulting stock level would fall below zero, driving inventory to -8978.

## Suggested Fix
In ChaosScheduler.applyWarehouseDelta() (BackgroundWorker), add a pre-application guard that validates the resulting stock level before invoking InventoryService.hardDeduct(). In InventoryService.hardDeduct(), enforce a floor check and reject or quarantine deltas that would produce negative stock: e.g., `if (currentStock - delta < 0) { throw new InvalidStockDeltaException("Delta would result in negative stock: " + (currentStock - delta)); }`. Also add a config key `inventory.allow-negative-stock=false` to enforce this at the service boundary.
