package com.marketplace.event;

import com.marketplace.observer.Event;
import com.marketplace.observer.EventType;

/** Aggregated event: a category has collected another full window of price drops. */
public record CategoryDropTrend(String categoryId, int dropCount) implements Event {
    public static final EventType<CategoryDropTrend> TYPE = EventType.named("CATEGORY_DROP_TREND");

    @Override
    public EventType<CategoryDropTrend> type() {
        return TYPE;
    }
}
