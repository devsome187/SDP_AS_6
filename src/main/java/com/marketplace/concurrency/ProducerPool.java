package com.marketplace.concurrency;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Runs producer tasks on an injected executor and shuts it down gracefully.
 *
 * <p>Events are delivered synchronously on the producer's thread, so once every task has
 * finished every event has reached every listener: nothing is buffered that could be lost.
 * {@link #close()} stops accepting work, waits for running tasks and only then interrupts.
 */
public final class ProducerPool implements AutoCloseable {
    private final ExecutorService executor;
    private final Duration shutdownTimeout;

    public ProducerPool(ExecutorService executor, Duration shutdownTimeout) {
        this.executor = executor;
        this.shutdownTimeout = shutdownTimeout;
    }

    /** Submits all tasks and blocks until each has completed; rethrows the first failure. */
    public void runAll(List<? extends Runnable> producers) throws InterruptedException {
        List<Future<?>> futures = producers.stream().<Future<?>>map(executor::submit).toList();
        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (ExecutionException e) {
                throw new IllegalStateException("Producer failed", e.getCause());
            }
        }
    }

    @Override
    public void close() throws InterruptedException {
        executor.shutdown();
        if (!executor.awaitTermination(shutdownTimeout.toMillis(), TimeUnit.MILLISECONDS)) {
            executor.shutdownNow();
            throw new IllegalStateException("Producers did not finish within " + shutdownTimeout);
        }
    }
}
