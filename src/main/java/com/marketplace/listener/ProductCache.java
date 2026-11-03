package com.marketplace.listener;

import com.marketplace.event.PriceChanged;
import com.marketplace.event.StockChanged;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Read-side cache of the latest price and stock per SKU, e.g. for product pages.
 * Producers may deliver events for the same SKU out of order, so every update keeps
 * whichever value carries the higher per-SKU version.
 */
public final class ProductCache {
    private final Map<String, Versioned<BigDecimal>> prices = new ConcurrentHashMap<>();
    private final Map<String, Versioned<Integer>> stock = new ConcurrentHashMap<>();

    public void onPriceChanged(PriceChanged event) {
        prices.merge(event.sku(), new Versioned<>(event.newPrice(), event.version()), Versioned::newer);
    }

    public void onStockChanged(StockChanged event) {
        stock.merge(event.sku(), new Versioned<>(event.newQuantity(), event.version()), Versioned::newer);
    }

    public Optional<BigDecimal> price(String sku) {
        return Optional.ofNullable(prices.get(sku)).map(Versioned::value);
    }

    public Optional<Integer> stock(String sku) {
        return Optional.ofNullable(stock.get(sku)).map(Versioned::value);
    }

    private record Versioned<V>(V value, long version) {
        Versioned<V> newer(Versioned<V> other) {
            return other.version > version ? other : this;
        }
    }
}
