package com.marketplace.event;

import com.marketplace.observer.Event;
import com.marketplace.observer.EventType;

/**
 * @param version per-SKU sequence number; lets listeners ignore events that arrive out of order
 */
public record StockChanged(String sku, int oldQuantity, int newQuantity, long version) implements Event {
    public static final EventType<StockChanged> TYPE = EventType.named("STOCK_CHANGED");

    @Override
    public EventType<StockChanged> type() {
        return TYPE;
    }

    public boolean isBackInStock() {
        return oldQuantity == 0 && newQuantity > 0;
    }
}
