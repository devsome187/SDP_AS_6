package com.marketplace.feed;

import com.marketplace.event.StockChanged;
import com.marketplace.observer.EventType;
import com.marketplace.observer.Observer;
import com.marketplace.observer.ObserverRegistry;
import com.marketplace.observer.Subject;
import com.marketplace.observer.Subscription;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Subject #2: warehouse stock of every SKU. Stock never goes below zero. */
public final class InventoryFeed implements Subject<StockChanged> {
    private final ObserverRegistry<StockChanged> registry;
    private final Map<String, StockLevel> levels = new ConcurrentHashMap<>();

    public InventoryFeed(ObserverRegistry<StockChanged> registry, Map<String, Integer> initialStock) {
        this.registry = registry;
        initialStock.forEach((sku, quantity) -> levels.put(sku, new StockLevel(quantity, quantity, 0)));
    }

    @Override
    public <T extends StockChanged> Subscription subscribe(EventType<T> type, Observer<? super T> observer) {
        return registry.subscribe(type, observer);
    }

    public void adjustStock(String sku, int delta) {
        StockLevel updated = levels.computeIfPresent(sku,
                (key, current) -> new StockLevel(Math.max(0, current.quantity() + delta), current.quantity(), current.version() + 1));
        if (updated == null) {
            throw new IllegalArgumentException("Unknown SKU: " + sku);
        }
        registry.publish(new StockChanged(sku, updated.previousQuantity(), updated.quantity(), updated.version()));
    }

    private record StockLevel(int quantity, int previousQuantity, long version) {
    }
}
