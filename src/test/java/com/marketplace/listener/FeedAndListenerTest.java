package com.marketplace.listener;

import com.marketplace.TestCatalog;
import com.marketplace.event.CategoryDropTrend;
import com.marketplace.event.PriceChanged;
import com.marketplace.event.StockChanged;
import com.marketplace.feed.InventoryFeed;
import com.marketplace.feed.PriceFeed;
import com.marketplace.observer.ObserverRegistry;
import com.marketplace.visitor.CatalogIndex;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.marketplace.TestCatalog.money;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FeedAndListenerTest {

    @Test
    void priceFeedPublishesOldAndNewPriceWithIncreasingVersion() {
        PriceFeed feed = new PriceFeed(new ObserverRegistry<>(), Map.of("L1", money("1000.00")));
        List<PriceChanged> received = new ArrayList<>();
        feed.subscribe(PriceChanged.TYPE, received::add);

        feed.setPrice("L1", money("750.00"));
        feed.setPrice("L1", money("800.00"));

        assertEquals(new PriceChanged("L1", money("1000.00"), money("750.00"), 1), received.get(0));
        assertEquals(new PriceChanged("L1", money("750.00"), money("800.00"), 2), received.get(1));
        assertEquals(0, money("25.00").compareTo(received.get(0).dropPercent()));
        assertEquals(money("800.00"), feed.currentPrice("L1"));
    }

    @Test
    void feedsRejectUnknownSkus() {
        PriceFeed prices = new PriceFeed(new ObserverRegistry<>(), Map.of());
        InventoryFeed stock = new InventoryFeed(new ObserverRegistry<>(), Map.of());

        assertThrows(IllegalArgumentException.class, () -> prices.setPrice("NOPE", BigDecimal.ONE));
        assertThrows(IllegalArgumentException.class, () -> stock.adjustStock("NOPE", 1));
    }

    @Test
    void inventoryNeverGoesBelowZero() {
        InventoryFeed feed = new InventoryFeed(new ObserverRegistry<>(), Map.of("P1", 2));
        List<StockChanged> received = new ArrayList<>();
        feed.subscribe(StockChanged.TYPE, received::add);

        feed.adjustStock("P1", -5);
        feed.adjustStock("P1", 3);

        assertEquals(new StockChanged("P1", 2, 0, 1), received.get(0));
        assertEquals(new StockChanged("P1", 0, 3, 2), received.get(1));
    }

    @Test
    void wishlistNotifiesTargetPriceOnceAndEveryRestock() {
        WishlistNotifier wishlist = new WishlistNotifier();
        wishlist.addWish("alice", "L1", money("900.00"));

        wishlist.onPriceChanged(new PriceChanged("L1", money("1000.00"), money("950.00"), 1));
        wishlist.onPriceChanged(new PriceChanged("L1", money("950.00"), money("900.00"), 2));
        wishlist.onPriceChanged(new PriceChanged("L1", money("900.00"), money("850.00"), 3));
        wishlist.addWish("bob", "P1", money("1.00"));
        wishlist.onStockChanged(new StockChanged("P1", 0, 4, 1));
        wishlist.onStockChanged(new StockChanged("P1", 4, 6, 2));

        assertEquals(List.of("alice: L1 is now 900.00", "bob: P1 is back in stock"), wishlist.notifications());
    }

    @Test
    void priceDropAlertUsesInclusiveThreshold() {
        PriceDropAlert alert = new PriceDropAlert(money("10.00"));

        alert.onPriceChanged(new PriceChanged("A", money("100.00"), money("91.00"), 1)); // 9 %
        alert.onPriceChanged(new PriceChanged("A", money("100.00"), money("90.00"), 2)); // 10 %
        alert.onPriceChanged(new PriceChanged("A", money("90.00"), money("120.00"), 3)); // rise

        assertEquals(1, alert.alerts().size());
        assertEquals(2, alert.alerts().get(0).version());
    }

    @Test
    void productCacheKeepsNewestVersionWhenEventsArriveOutOfOrder() {
        ProductCache cache = new ProductCache();

        cache.onPriceChanged(new PriceChanged("L1", money("1000.00"), money("700.00"), 3));
        cache.onPriceChanged(new PriceChanged("L1", money("1000.00"), money("900.00"), 2));
        cache.onStockChanged(new StockChanged("L1", 5, 4, 1));

        assertEquals(money("700.00"), cache.price("L1").orElseThrow());
        assertEquals(4, cache.stock("L1").orElseThrow());
        assertEquals(java.util.Optional.empty(), cache.price("UNKNOWN"));
    }

    @Test
    void aggregatorRepublishesOneTrendPerFullWindowOfDropsInACategory() {
        CategoryDropAggregator aggregator = new CategoryDropAggregator(
                new ObserverRegistry<>(), CatalogIndex.of(TestCatalog.CATALOG), 2);
        List<CategoryDropTrend> trends = new ArrayList<>();
        aggregator.subscribe(CategoryDropTrend.TYPE, trends::add);

        aggregator.onPriceChanged(new PriceChanged("L1", money("10.00"), money("9.00"), 1));
        aggregator.onPriceChanged(new PriceChanged("L1", money("9.00"), money("12.00"), 2)); // rise: ignored
        aggregator.onPriceChanged(new PriceChanged("P1", money("10.00"), money("9.00"), 1)); // other category
        aggregator.onPriceChanged(new PriceChanged("D1", money("10.00"), money("9.00"), 1));

        assertEquals(List.of(new CategoryDropTrend("laptops", 2)), trends);
    }
}
