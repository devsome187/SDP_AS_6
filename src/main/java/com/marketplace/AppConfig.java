package com.marketplace;

import java.math.BigDecimal;

/**
 * Runtime settings. Positional arguments: producer threads, events per producer.
 */
public record AppConfig(
        int producerThreads,
        int eventsPerProducer,
        int dropWindow,
        BigDecimal alertThresholdPercent,
        int lowStockThreshold) {

    private static final int DEFAULT_THREADS = 4;
    private static final int DEFAULT_EVENTS_PER_PRODUCER = 1_000;
    private static final int DROPS_PER_TREND = 50;
    private static final BigDecimal ALERT_DROP_PERCENT = new BigDecimal("8.00");
    private static final int LOW_STOCK_THRESHOLD = 5;

    public AppConfig {
        if (producerThreads < 1 || eventsPerProducer < 0) {
            throw new IllegalArgumentException("Need at least one producer thread and a non-negative event count");
        }
    }

    public static AppConfig fromArgs(String[] args) {
        int threads = args.length > 0 ? Integer.parseInt(args[0]) : DEFAULT_THREADS;
        int events = args.length > 1 ? Integer.parseInt(args[1]) : DEFAULT_EVENTS_PER_PRODUCER;
        return new AppConfig(threads, events, DROPS_PER_TREND, ALERT_DROP_PERCENT, LOW_STOCK_THRESHOLD);
    }
}
