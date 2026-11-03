package com.marketplace.concurrency;

import com.marketplace.event.PriceChanged;
import com.marketplace.event.StockChanged;
import com.marketplace.feed.InventoryFeed;
import com.marketplace.feed.PriceFeed;
import com.marketplace.listener.MarketAnalytics;
import com.marketplace.listener.ProductCache;
import com.marketplace.listener.WishlistNotifier;
import com.marketplace.observer.ObserverRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

import static com.marketplace.TestCatalog.money;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Timeout(30)
class StressTest {
    private static final int THREADS = 4;
    private static final int EVENTS_PER_THREAD = 1_000;
    private static final List<String> SKUS = List.of("A", "B", "C");

    private final PriceFeed priceFeed = new PriceFeed(new ObserverRegistry<>(),
            Map.of("A", money("100.00"), "B", money("200.00"), "C", money("300.00")));
    private final InventoryFeed inventoryFeed = new InventoryFeed(new ObserverRegistry<>(), Map.of("A", 10, "B", 10));

    private List<MarketTrafficGenerator> producers() {
        return IntStream.range(0, THREADS)
                .mapToObj(seed -> new MarketTrafficGenerator(priceFeed, inventoryFeed, SKUS, List.of("A", "B"),
                        EVENTS_PER_THREAD, seed))
                .toList();
    }

    @Test
    void fourThreadsTimesThousandEventsAreAllDeliveredExactlyOnce() throws Exception {
        MarketAnalytics analytics = new MarketAnalytics();
        ProductCache cache = new ProductCache();
        priceFeed.subscribe(PriceChanged.TYPE, analytics::onEvent);
        priceFeed.subscribe(PriceChanged.TYPE, cache::onPriceChanged);
        inventoryFeed.subscribe(StockChanged.TYPE, analytics::onEvent);
        ExecutorService executor = Executors.newFixedThreadPool(THREADS);

        try (ProducerPool pool = new ProducerPool(executor, Duration.ofSeconds(10))) {
            pool.runAll(producers());
        }

        assertEquals(THREADS * EVENTS_PER_THREAD, analytics.totalEvents());
        assertEquals(analytics.totalEvents(), analytics.count(PriceChanged.TYPE) + analytics.count(StockChanged.TYPE));
        assertTrue(executor.isTerminated(), "no producer thread may be left running");
        for (String sku : SKUS) {
            assertEquals(priceFeed.currentPrice(sku), cache.price(sku).orElseThrow(), "cache converges to the feed");
        }
    }

    @Test
    void wishIsFulfilledExactlyOnceUnderConcurrentPriceChanges() throws Exception {
        WishlistNotifier wishlist = new WishlistNotifier();
        wishlist.addWish("alice", "A", money("1000.00")); // every price satisfies it
        priceFeed.subscribe(PriceChanged.TYPE, wishlist::onPriceChanged);

        try (ProducerPool pool = new ProducerPool(Executors.newFixedThreadPool(THREADS), Duration.ofSeconds(10))) {
            pool.runAll(producers());
        }

        assertEquals(1, wishlist.notifications().size());
    }

    @Test
    void producerFailureIsReportedAndPoolStillShutsDown() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        Runnable failing = () -> priceFeed.setPrice("UNKNOWN", money("1.00"));

        try (ProducerPool pool = new ProducerPool(executor, Duration.ofSeconds(5))) {
            assertThrows(IllegalStateException.class, () -> pool.runAll(List.of(failing)));
        }

        assertTrue(executor.isTerminated());
    }
}
