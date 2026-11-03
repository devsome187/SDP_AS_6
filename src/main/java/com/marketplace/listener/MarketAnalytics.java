package com.marketplace.listener;

import com.marketplace.observer.Event;
import com.marketplace.observer.EventType;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

/** Counts events per type. LongAdder keeps the hot path contention-free under many producers. */
public final class MarketAnalytics {
    private final Map<EventType<?>, LongAdder> counters = new ConcurrentHashMap<>();

    public void onEvent(Event event) {
        counters.computeIfAbsent(event.type(), type -> new LongAdder()).increment();
    }

    public long count(EventType<?> type) {
        LongAdder counter = counters.get(type);
        return counter == null ? 0 : counter.sum();
    }

    public long totalEvents() {
        return counters.values().stream().mapToLong(LongAdder::sum).sum();
    }
}
