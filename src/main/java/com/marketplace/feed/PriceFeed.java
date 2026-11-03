package com.marketplace.feed;

import com.marketplace.event.PriceChanged;
import com.marketplace.observer.EventType;
import com.marketplace.observer.Observer;
import com.marketplace.observer.ObserverRegistry;
import com.marketplace.observer.Subject;
import com.marketplace.observer.Subscription;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.UnaryOperator;

/** Subject #1: the live price of every SKU. Safe to call from many producer threads. */
public final class PriceFeed implements Subject<PriceChanged> {
    private final ObserverRegistry<PriceChanged> registry;
    private final Map<String, PricePoint> prices = new ConcurrentHashMap<>();

    public PriceFeed(ObserverRegistry<PriceChanged> registry, Map<String, BigDecimal> initialPrices) {
        this.registry = registry;
        initialPrices.forEach((sku, price) -> prices.put(sku, new PricePoint(price, price, 0)));
    }

    @Override
    public <T extends PriceChanged> Subscription subscribe(EventType<T> type, Observer<? super T> observer) {
        return registry.subscribe(type, observer);
    }

    public void setPrice(String sku, BigDecimal newPrice) {
        reprice(sku, current -> newPrice);
    }

    /**
     * Atomically derives the new price from the current one. The event is built from the
     * stored result and published after the map update, never inside it.
     */
    public void reprice(String sku, UnaryOperator<BigDecimal> rule) {
        PricePoint updated = prices.computeIfPresent(sku,
                (key, current) -> new PricePoint(rule.apply(current.price()), current.price(), current.version() + 1));
        if (updated == null) {
            throw new IllegalArgumentException("Unknown SKU: " + sku);
        }
        registry.publish(new PriceChanged(sku, updated.previousPrice(), updated.price(), updated.version()));
    }

    public BigDecimal currentPrice(String sku) {
        PricePoint point = prices.get(sku);
        if (point == null) {
            throw new IllegalArgumentException("Unknown SKU: " + sku);
        }
        return point.price();
    }

    private record PricePoint(BigDecimal price, BigDecimal previousPrice, long version) {
    }
}
