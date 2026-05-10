/**
 * Fixed ChaosScheduler with connection pool ceiling increase and task-level throttling.
 *
 * @fix-for ChaosScheduler in BackgroundWorker exhibiting resource pressure due to connection pool
 *          near capacity (8/10) and metrics collector buffer lag (30ms) under high concurrent workload.
 * @confidence HIGH — root cause is clearly identified as unbounded concurrent task dispatch consuming
 *             pool connections in a single cycle; semaphore guard directly limits concurrent acquisition.
 * @blast-radius MEDIUM — changes affect task dispatch throughput and connection pool sizing; existing
 *              tasks will queue behind semaphore permits rather than fail, preserving correctness while
 *              reducing peak resource consumption. Throughput per cycle may decrease slightly by design.
 */
package com.example.background;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class ChaosScheduler {

    private static final Logger log = LoggerFactory.getLogger(ChaosScheduler.class);

    // Raised from 10 to 20 to provide headroom beyond observed 8/10 saturation point
    private static final int DB_POOL_MAX_SIZE = Integer.getInteger("db.pool.maxSize", 20);

    // Semaphore capped at 8 so a single dispatch cycle can never exhaust the full pool,
    // leaving at least (DB_POOL_MAX_SIZE - 8) connections free for other concurrent callers
    private static final int MAX_CONCURRENT_TASK_SLOTS = 8;

    private final Semaphore taskSlots = new Semaphore(MAX_CONCURRENT_TASK_SLOTS, true); // fair=true prevents starvation

    private final DataSource dataSource;                  // injected pool configured with DB_POOL_MAX_SIZE
    private final ExecutorService workerPool;             // bounded executor to back the semaphore contract
    private final MetricsCollector metricsCollector;      // downstream metrics pipeline
    private final AuditEventPersister auditPersister;     // audit sink
    private final DeadLetterPruner deadLetterPruner;      // dead-letter cleanup
    private final AtomicInteger cycleCounter = new AtomicInteger(0);

    // Metrics for observability — track permit wait time to detect future saturation early
    private final AtomicInteger semaphoreTimeoutCount = new AtomicInteger(0);

    public ChaosScheduler(DataSource dataSource,
                          MetricsCollector metricsCollector,
                          AuditEventPersister auditPersister,
                          DeadLetterPruner deadLetterPruner) {
        this.dataSource = dataSource;
        this.metricsCollector = metricsCollector;
        this.auditPersister = auditPersister;
        this.deadLetterPruner = deadLetterPruner;
        // Thread pool sized to match semaphore slots so excess threads don't spin-wait on permits
        this.workerPool = new ThreadPoolExecutor(
                MAX_CONCURRENT_TASK_SLOTS,          // corePoolSize matches semaphore to avoid idle threads
                MAX_CONCURRENT_TASK_SLOTS,          // maxPoolSize equals core — no unbounded burst threads
                60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(256),     // bounded queue prevents OOM under sustained backpressure
                new ThreadFactory() {
                    private final AtomicInteger idx = new AtomicInteger(0);
                    @Override public Thread newThread(Runnable r) {
                        Thread t = new Thread(r, "chaos-worker-" + idx.getAndIncrement());
                        t.setDaemon(true); // daemon so JVM shutdown is not blocked by lingering tasks
                        return t;
                    }
                },
                new ThreadPoolExecutor.CallerRunsPolicy() //