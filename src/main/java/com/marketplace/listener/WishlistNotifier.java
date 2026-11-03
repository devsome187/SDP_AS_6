package com.marketplace.listener;

import com.marketplace.event.PriceChanged;
import com.marketplace.event.StockChanged;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Notifies customers when a wished-for product reaches their target price (once per wish)
 * and every time a wished-for product comes back in stock.
 */
public final class WishlistNotifier {
    private final Map<String, Set<Wish>> wishesBySku = new ConcurrentHashMap<>();
    private final Queue<String> notifications = new ConcurrentLinkedQueue<>();

    public void addWish(String customerId, String sku, BigDecimal targetPrice) {
        wishesBySku.computeIfAbsent(sku, key -> ConcurrentHashMap.newKeySet())
                .add(new Wish(customerId, targetPrice));
    }

    public void onPriceChanged(PriceChanged event) {
        Set<Wish> wishes = wishesFor(event.sku());
        for (Wish wish : wishes) {
            boolean targetReached = event.newPrice().compareTo(wish.targetPrice()) <= 0;
            // remove() succeeds for exactly one thread, so a wish is fulfilled exactly once
            if (targetReached && wishes.remove(wish)) {
                notifications.add(wish.customerId() + ": " + event.sku() + " is now " + event.newPrice());
            }
        }
    }

    public void onStockChanged(StockChanged event) {
        if (!event.isBackInStock()) {
            return;
        }
        for (Wish wish : wishesFor(event.sku())) {
            notifications.add(wish.customerId() + ": " + event.sku() + " is back in stock");
        }
    }

    public List<String> notifications() {
        return List.copyOf(notifications);
    }

    private Set<Wish> wishesFor(String sku) {
        return wishesBySku.getOrDefault(sku, Set.of());
    }

    private record Wish(String customerId, BigDecimal targetPrice) {
    }
}
