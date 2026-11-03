package com.marketplace;

import com.marketplace.catalog.Catalog;
import com.marketplace.catalog.PhysicalProduct;
import com.marketplace.catalog.Product;
import com.marketplace.concurrency.MarketTrafficGenerator;
import com.marketplace.concurrency.ProducerPool;
import com.marketplace.event.CategoryDropTrend;
import com.marketplace.event.CategoryReport;
import com.marketplace.event.PriceChanged;
import com.marketplace.event.StockChanged;
import com.marketplace.feed.InventoryFeed;
import com.marketplace.feed.PriceFeed;
import com.marketplace.integration.CategoryInspectionService;
import com.marketplace.listener.CategoryDropAggregator;
import com.marketplace.listener.MarketAnalytics;
import com.marketplace.listener.PriceDropAlert;
import com.marketplace.listener.ProductCache;
import com.marketplace.listener.ReportArchive;
import com.marketplace.listener.WishlistNotifier;
import com.marketplace.observer.ObserverRegistry;
import com.marketplace.visitor.CatalogIndex;
import com.marketplace.visitor.CatalogStatisticsVisitor;
import com.marketplace.visitor.ProductFilterVisitor;
import com.marketplace.visitor.ShippingCostVisitor;
import com.marketplace.visitor.TaxVisitor;
import com.marketplace.visitor.XmlExportVisitor;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Composition root: the only place that creates and wires objects. Everything else
 * receives its collaborators through constructors.
 */
public final class App {

    public static void main(String[] args) throws InterruptedException {
        AppConfig config = AppConfig.fromArgs(args);
        Catalog catalog = SampleCatalog.create();
        CatalogIndex catalogIndex = CatalogIndex.of(catalog);

        List<Product> products = catalog.accept(new ProductFilterVisitor(product -> true));
        List<PhysicalProduct> physicalProducts = products.stream()
                .filter(PhysicalProduct.class::isInstance).map(PhysicalProduct.class::cast).toList();

        PriceFeed priceFeed = new PriceFeed(new ObserverRegistry<>(), toMap(products, Product::price));
        InventoryFeed inventoryFeed = new InventoryFeed(new ObserverRegistry<>(), toMap(physicalProducts, PhysicalProduct::stock));

        TaxVisitor taxVisitor = new TaxVisitor(new BigDecimal("0.12"), new BigDecimal("0.05"));
        ShippingCostVisitor shippingVisitor = new ShippingCostVisitor(new BigDecimal("5.00"), new BigDecimal("2.00"), new BigDecimal("1.50"));
        // the catalog tree holds the opening stock; the live level comes from the inventory feed
        ProductFilterVisitor restockFilter = new ProductFilterVisitor(product -> product instanceof PhysicalProduct
                && inventoryFeed.currentStock(product.sku()) < config.lowStockThreshold());

        WishlistNotifier wishlist = new WishlistNotifier();
        PriceDropAlert dropAlert = new PriceDropAlert(config.alertThresholdPercent());
        MarketAnalytics analytics = new MarketAnalytics();
        ProductCache cache = new ProductCache();
        CategoryDropAggregator aggregator = new CategoryDropAggregator(new ObserverRegistry<>(), catalogIndex, config.dropWindow());
        CategoryInspectionService inspection = new CategoryInspectionService(new ObserverRegistry<>(), catalogIndex,
                taxVisitor, shippingVisitor, restockFilter, new XmlExportVisitor());
        ReportArchive archive = new ReportArchive();

        priceFeed.subscribe(PriceChanged.TYPE, wishlist::onPriceChanged);
        priceFeed.subscribe(PriceChanged.TYPE, dropAlert::onPriceChanged);
        priceFeed.subscribe(PriceChanged.TYPE, analytics::onEvent);
        priceFeed.subscribe(PriceChanged.TYPE, cache::onPriceChanged);
        priceFeed.subscribe(PriceChanged.TYPE, aggregator::onPriceChanged);
        inventoryFeed.subscribe(StockChanged.TYPE, wishlist::onStockChanged);
        inventoryFeed.subscribe(StockChanged.TYPE, analytics::onEvent);
        inventoryFeed.subscribe(StockChanged.TYPE, cache::onStockChanged);
        aggregator.subscribe(CategoryDropTrend.TYPE, inspection::onDropTrend);
        aggregator.subscribe(CategoryDropTrend.TYPE, analytics::onEvent);
        inspection.subscribe(CategoryReport.TYPE, archive::onReport);
        inspection.subscribe(CategoryReport.TYPE, analytics::onEvent);

        wishlist.addWish("alice", "LAP-001", new BigDecimal("1000.00"));
        wishlist.addWish("bob", "PH-002", new BigDecimal("1100.00"));

        List<String> pricedSkus = products.stream().map(Product::sku).toList();
        List<String> stockedSkus = physicalProducts.stream().map(Product::sku).toList();
        List<MarketTrafficGenerator> producers = IntStream.range(0, config.producerThreads())
                .mapToObj(seed -> new MarketTrafficGenerator(priceFeed, inventoryFeed, pricedSkus, stockedSkus,
                        config.eventsPerProducer(), seed))
                .toList();

        try (ProducerPool pool = new ProducerPool(Executors.newFixedThreadPool(config.producerThreads()), Duration.ofSeconds(10))) {
            pool.runAll(producers);
        }

        System.out.println("Catalog: " + CatalogStatisticsVisitor.collect(catalog));
        System.out.println("Tax for one unit of every product: " + catalog.accept(taxVisitor));
        System.out.println("Events: price=" + analytics.count(PriceChanged.TYPE)
                + " stock=" + analytics.count(StockChanged.TYPE)
                + " trends=" + analytics.count(CategoryDropTrend.TYPE)
                + " reports=" + analytics.count(CategoryReport.TYPE));
        System.out.println("Price-drop alerts: " + dropAlert.alerts().size());
        System.out.println("Cached LAP-001 price: " + cache.price("LAP-001").orElseThrow()
                + ", feed price: " + priceFeed.currentPrice("LAP-001"));
        wishlist.notifications().stream().limit(5).forEach(note -> System.out.println("Notify " + note));
        archive.reports().stream().reduce((first, second) -> second).ifPresent(report -> {
            System.out.println("Last report: " + report.categoryId() + " tax=" + report.taxTotal()
                    + " shipping=" + report.shippingTotal() + " restock=" + report.restockSkus());
            System.out.println(report.xml());
        });
    }

    private static <P extends Product, V> Map<String, V> toMap(List<P> products, Function<P, V> value) {
        return products.stream().collect(Collectors.toMap(Product::sku, value));
    }
}
