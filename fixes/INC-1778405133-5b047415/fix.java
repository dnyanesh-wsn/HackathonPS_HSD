/**
 * Fix for ChaosScheduler and InventoryService negative stock vulnerability.
 *
 * @fix-for STOCK_LEVEL_ANOMALY - ChaosScheduler applied warehouse feed delta of qty=999
 *          for PROD-005 without pre-validating delta magnitude or resulting stock level,
 *          driving inventory to -5985 and persisting NEGATIVE_STOCK state.
 * @confidence HIGH - Root cause is clearly a missing pre-deduction guard and missing
 *             delta magnitude threshold check; fix directly addresses both failure points
 *             in the causal chain at steps 1 and 2.
 * @blast-radius MEDIUM - Changes affect ChaosScheduler.applyWarehouseFeedDelta() and
 *               InventoryService.deductStock(); all callers of deductStock() will now
 *               receive InvalidStockOperationException on negative-result deductions,
 *               requiring callers to handle the checked exception or let it propagate.
 */

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.concurrent.locks.ReentrantLock;

// ---------------------------------------------------------------------------
// Custom exception – lives in its own file in production; inlined here for
// self-containment as required by the fix rules.
// ---------------------------------------------------------------------------
class InvalidStockOperationException extends RuntimeException {
    // Unchecked so Spring @Transactional rolls back automatically on throw.
    public InvalidStockOperationException(String message) {
        super(message);
    }
}

// ---------------------------------------------------------------------------
// InventoryService – owns the authoritative stock mutation path.
// ---------------------------------------------------------------------------
@Component
class InventoryService {

    private static final Logger log = LoggerFactory.getLogger(InventoryService.class);

    // In production this would be a JPA repository / DB call; modelled as a
    // simple in-memory store here so the fix is self-contained.
    private final java.util.concurrent.ConcurrentHashMap<String, Integer> stockStore =
            new java.util.concurrent.ConcurrentHashMap<>();

    // Per-product lock prevents TOCTOU race between the guard read and the write.
    private final java.util.concurrent.ConcurrentHashMap<String, ReentrantLock> productLocks =
            new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * Deducts {@code deductionQty} units from the current stock of {@code productId}.
     *
     * <p>A pre-deduction guard now rejects any operation that would drive stock
     * below zero, closing the gap identified in causal-chain step 2.
     *
     * @param productId    the product whose stock is being reduced
     * @param deductionQty the number of units to remove (must be &gt; 0)
     * @throws InvalidStockOperationException if the deduction would result in negative stock
     */
    @Transactional // rolls back automatically if InvalidStockOperationException is thrown
    public int deductStock(String productId, int deductionQty) {

        if (deductionQty <= 0) { // reject nonsensical deduction values before touching stock
            throw new InvalidStockOperationException(
                    "Deduction quantity must be positive, got " + deductionQty
                    + " for product " + productId);
        }

        // Acquire a per-product lock so the guard + write is atomic under concurrency.
        ReentrantLock lock = productLocks.computeIfAbsent(productId, k -> new ReentrantLock());
        lock.lock();
        try {
            int currentStock = stockStore.getOrDefault(productId, 0); // read authoritative value inside lock

            // ---------------------------------------------------------------
            // PRE-DEDUCTION GUARD – this is the primary fix for causal step 2.
            // Without this check the deduction was committed even when it drove
            // stock to -5985; now it is rejected before any mutation occurs.
            // ---------------------------------------------------------------