package com.marketplace.event;

import com.marketplace.observer.Event;
import com.marketplace.observer.EventType;

import java.math.BigDecimal;
import java.util.List;

/** Result of inspecting one category subtree after a drop trend: fed back into the system as an event. */
public record CategoryReport(
        String categoryId,
        int triggeringDropCount,
        BigDecimal taxTotal,
        BigDecimal shippingTotal,
        List<String> restockSkus,
        String xml) implements Event {

    public static final EventType<CategoryReport> TYPE = EventType.named("CATEGORY_REPORT");

    public CategoryReport {
        restockSkus = List.copyOf(restockSkus);
    }

    @Override
    public EventType<CategoryReport> type() {
        return TYPE;
    }
}
