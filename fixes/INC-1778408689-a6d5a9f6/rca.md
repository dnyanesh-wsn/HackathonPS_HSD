# RCA — INC-1778408689-a6d5a9f6

**Trace ID:** stock-sync-bac4c21a
**Service:** BackgroundWorker
**Confidence:** 88%
**Blast Radius:** HIGH

## Root Cause
InventoryService in InventoryService failed to validate stock floor bounds before applying the warehouse feed delta because it allowed a hard deduction of qty=999 against insufficient on-hand inventory for PROD-005, driving the stock counter to -1990 (NEGATIVE_STOCK).

## Suggested Fix
In InventoryService.applyHardDeduction(), add a pre-deduction guard that rejects any delta that would drive stock below zero, and in ChaosScheduler.applyFeedDelta(), treat INV_COUNTER_UNDERFLOW as a terminal error that rolls back and halts the sync. Example guard: `if (currentStock - qty < 0) throw new InventoryUnderflowException("Deduction would result in negative stock for " + productId);`
