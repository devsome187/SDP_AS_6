package com.marketplace.listener;

import com.marketplace.event.CategoryDropTrend;
import com.marketplace.event.PriceChanged;
import com.marketplace.observer.EventType;
import com.marketplace.observer.Observer;
import com.marketplace.observer.ObserverRegistry;
import com.marketplace.observer.Subject;
import com.marketplace.observer.Subscription;
import com.marketplace.visitor.CatalogIndex;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Chain link: listens to raw price changes and republishes aggregated data.
 * Every {@code windowSize}-th drop inside a category emits one {@link CategoryDropTrend}.
 */
public final class CategoryDropAggregator implements Subject<CategoryDropTrend> {
    private final ObserverRegistry<CategoryDropTrend> registry;
    private final CatalogIndex catalogIndex;
    private final int windowSize;
    private final Map<String, Integer> dropsByCategory = new ConcurrentHashMap<>();

    public CategoryDropAggregator(ObserverRegistry<CategoryDropTrend> registry, CatalogIndex catalogIndex, int windowSize) {
        if (windowSize < 1) {
            throw new IllegalArgumentException("windowSize must be positive");
        }
        this.registry = registry;
        this.catalogIndex = catalogIndex;
        this.windowSize = windowSize;
    }

    @Override
    public <T extends CategoryDropTrend> Subscription subscribe(EventType<T> type, Observer<? super T> observer) {
        return registry.subscribe(type, observer);
    }

    public void onPriceChanged(PriceChanged event) {
        if (!event.isDrop()) {
            return;
        }
        String categoryId = catalogIndex.categoryIdOf(event.sku());
        // merge is atomic per key: every count value is produced by exactly one thread,
        // so each full window publishes exactly one trend
        int drops = dropsByCategory.merge(categoryId, 1, Integer::sum);
        if (drops % windowSize == 0) {
            registry.publish(new CategoryDropTrend(categoryId, drops));
        }
    }
}
