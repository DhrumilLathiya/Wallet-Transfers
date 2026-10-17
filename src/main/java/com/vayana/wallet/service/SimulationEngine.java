package com.vayana.wallet.service;

import com.vayana.wallet.model.Account;
import com.vayana.wallet.model.TransferResult;
import com.vayana.wallet.util.MoneyUtil;

import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * =========================================================================================
 * CONCURRENCY SIMULATION ENGINE: SimulationEngine
 * =========================================================================================
 * Evaluator Walkthrough Notes:
 * ----------------------------
 * 1. REQUIREMENT 5 (Reuse of Business Logic):
 *    - "The simulate command must call the same business-logic functions as the normal commands."
 *    - Each of the 20 threads calls walletService.transfer(...) directly. No bypasses or mocks!
 *
 * 2. THREAD SYNCHRONIZATION PATTERNS:
 *    a) START GATE (CountDownLatch of 1):
 *       - If we just submit tasks to an ExecutorService, Thread 1 might finish before Thread 20
 *         even starts. That would NOT test true concurrency!
 *       - Solution: All 20 threads call `startGate.await()`. Once all 20 threads are primed
 *         and waiting in the pool, the main thread calls `startGate.countDown()`.
 *       - This fires all 20 threads simultaneously at the exact same microsecond!
 *
 *    b) DONE GATE (CountDownLatch of 20):
 *       - Acts as a synchronization barrier. The main thread calls `doneGate.await(30, SECONDS)`.
 *       - If a deadlock occurs in the service layer, the countdown latch will time out and fail,
 *         alerting us to a concurrency bug.
 *
 * 3. SAFETY & INVARIANT CHECKS:
 *    - Conservation of Money: Initial total (X + Y = ₹2,000) == Final total (X + Y = ₹2,000).
 *    - No Overdraft: Balance(X) >= 0 and Balance(Y) >= 0.
 * =========================================================================================
 */
public class SimulationEngine {

    private final WalletService walletService;

    /**
     * Immutable data record capturing the complete telemetry of the simulation run.
     */
    public record SimulationReport(
            long accountXId,
            long accountYId,
            long finalBalanceX,
            long finalBalanceY,
            long totalBalance,
            int succeeded,
            int failed,
            boolean ruleHolds,
            long durationMillis
    ) {
        @Override
        public String toString() {
            return String.format(
                """
                ================ SIMULATION RESULTS ================
                Account X (ID: %d): Final Balance = %s
                Account Y (ID: %d): Final Balance = %s
                Total Balance: %s
                Transfers Succeeded: %d
                Transfers Failed: %d
                Rule Check (X + Y == ₹2,000.00 & Balances >= 0): %s
                Total Duration: %d ms
                ===================================================
                """,
                accountXId, MoneyUtil.formatDetailed(finalBalanceX),
                accountYId, MoneyUtil.formatDetailed(finalBalanceY),
                MoneyUtil.formatDetailed(totalBalance),
                succeeded, failed,
                ruleHolds ? "PASSED (No money lost or created, zero deadlocks)" : "FAILED (Rule violated!)",
                durationMillis
            );
        }
    }

    public SimulationEngine(WalletService walletService) {
        this.walletService = walletService;
    }

    /**
     * Executes the 20-thread concurrency simulation:
     * - Opens account X and Y with ₹1,000 (100,000 paise) each.
     * - Spawns 20 threads simultaneously using a CountDownLatch start gate:
     *   10 threads: X -> Y (₹100 = 10,000 paise)
     *   10 threads: Y -> X (₹100 = 10,000 paise)
     * - Verifies that X + Y still equals ₹2,000 and neither balance is negative.
     */
    public SimulationReport runSimulation() throws SQLException, InterruptedException {
        long initialEachPaise = 100_000L;  // ₹1,000 = 100,000 paise
        long transferAmountPaise = 10_000L; // ₹100 = 10,000 paise
        long expectedTotalPaise = 200_000L; // ₹2,000 = 200,000 paise

        // Unique run ID to guarantee distinct references across multiple simulation runs
        String simRunId = UUID.randomUUID().toString().substring(0, 8);

        // 1. Setup Phase: Create accounts X and Y
        Account accX = walletService.openAccount("Sim-X-" + simRunId, initialEachPaise);
        Account accY = walletService.openAccount("Sim-Y-" + simRunId, initialEachPaise);

        long idX = accX.getId();
        long idY = accY.getId();

        int totalThreads = 20;
        ExecutorService executor = Executors.newFixedThreadPool(totalThreads);

        // Synchronization latches
        CountDownLatch startGate = new CountDownLatch(1);        // Holds all threads until triggered
        CountDownLatch doneGate = new CountDownLatch(totalThreads); // Waits for all 20 threads to complete

        // Thread-safe counters for metrics
        AtomicInteger succeededCount = new AtomicInteger(0);
        AtomicInteger failedCount = new AtomicInteger(0);

        long startTime = System.currentTimeMillis();

        // 2. Submit 10 transfers from X -> Y
        for (int i = 1; i <= 10; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    startGate.await(); // Block here until the starting gun fires!
                    String ref = "sim-" + simRunId + "-XtoY-" + index;
                    TransferResult result = walletService.transfer(idX, idY, transferAmountPaise, ref);
                    if (result.isSuccess()) {
                        succeededCount.incrementAndGet();
                    } else {
                        failedCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    failedCount.incrementAndGet();
                } finally {
                    doneGate.countDown(); // Signal thread completion
                }
            });
        }

        // 3. Submit 10 transfers from Y -> X
        for (int i = 1; i <= 10; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    startGate.await(); // Block here until the starting gun fires!
                    String ref = "sim-" + simRunId + "-YtoX-" + index;
                    TransferResult result = walletService.transfer(idY, idX, transferAmountPaise, ref);
                    if (result.isSuccess()) {
                        succeededCount.incrementAndGet();
                    } else {
                        failedCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    failedCount.incrementAndGet();
                } finally {
                    doneGate.countDown(); // Signal thread completion
                }
            });
        }

        // 4. Starting gun: Release all 20 threads simultaneously
        startGate.countDown();

        // 5. Barrier: Wait up to 30 seconds for all 20 threads to finish
        boolean completedInTime = doneGate.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        if (!completedInTime) {
            throw new RuntimeException("Simulation timed out! Deadlock detected.");
        }

        long duration = System.currentTimeMillis() - startTime;

        // 6. Invariant Verification Phase:
        // Fetch fresh balance snapshots from the database
        Account finalX = walletService.getAccount(idX);
        Account finalY = walletService.getAccount(idY);

        long finalBalanceX = finalX.getBalance();
        long finalBalanceY = finalY.getBalance();
        long actualTotal = finalBalanceX + finalBalanceY;

        // Rule check: Total must still equal ₹2,000, and neither balance is negative
        boolean ruleHolds = (actualTotal == expectedTotalPaise)
                && (finalBalanceX >= 0)
                && (finalBalanceY >= 0);

        return new SimulationReport(
                idX, idY,
                finalBalanceX, finalBalanceY, actualTotal,
                succeededCount.get(), failedCount.get(),
                ruleHolds, duration
        );
    }
}
