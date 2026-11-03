package com.marketplace.observer;

/**
 * Handle returned by {@link Subject#subscribe}. Cancelling is idempotent and may be
 * called from any thread, including from inside the observer's own handler.
 */
@FunctionalInterface
public interface Subscription {
    void cancel();
}
