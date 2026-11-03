package com.marketplace.integration;

import com.marketplace.TestCatalog;
import com.marketplace.catalog.PhysicalProduct;
import com.marketplace.event.CategoryDropTrend;
import com.marketplace.event.CategoryReport;
import com.marketplace.event.PriceChanged;
import com.marketplace.feed.PriceFeed;
import com.marketplace.listener.CategoryDropAggregator;
import com.marketplace.listener.ReportArchive;
import com.marketplace.observer.ObserverRegistry;
import com.marketplace.visitor.CatalogIndex;
import com.marketplace.visitor.ProductFilterVisitor;
import com.marketplace.visitor.ShippingCostVisitor;
import com.marketplace.visitor.TaxVisitor;
import com.marketplace.visitor.XmlExportVisitor;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static com.marketplace.TestCatalog.money;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CategoryInspectionTest {
    private final CatalogIndex index = CatalogIndex.of(TestCatalog.CATALOG);
    private final CategoryInspectionService inspection = new CategoryInspectionService(
            new ObserverRegistry<>(), index,
            new TaxVisitor(new BigDecimal("0.12"), new BigDecimal("0.05")),
            new ShippingCostVisitor(money("5.00"), money("2.00"), money("1.50")),
            new ProductFilterVisitor(product -> product instanceof PhysicalProduct physical && physical.stock() < 8),
            new XmlExportVisitor());
    private final ReportArchive archive = new ReportArchive();

    @Test
    void trendRunsVisitorsOnTheAffectedSubtreeOnly() {
        inspection.subscribe(CategoryReport.TYPE, archive::onReport);

        inspection.onDropTrend(new CategoryDropTrend("laptops", 3));

        CategoryReport report = archive.reports().get(0);
        assertEquals("laptops", report.categoryId());
        assertEquals(3, report.triggeringDropCount());
        assertEquals(money("132.00"), report.taxTotal());      // L1 120.00 + D1 12.00
        assertEquals(money("9.00"), report.shippingTotal());   // L1 only
        assertEquals(List.of("L1"), report.restockSkus());     // stock 5 < 8
        assertTrue(report.xml().startsWith("<category id=\"laptops\""));
        assertFalse(report.xml().contains("P1"), "sibling subtree must not be visited");
    }

    @Test
    void priceDropsFlowThroughTheWholeChainIntoAReport() {
        PriceFeed feed = new PriceFeed(new ObserverRegistry<>(), Map.of("P1", money("500.00")));
        CategoryDropAggregator aggregator = new CategoryDropAggregator(new ObserverRegistry<>(), index, 2);
        feed.subscribe(PriceChanged.TYPE, aggregator::onPriceChanged);
        aggregator.subscribe(CategoryDropTrend.TYPE, inspection::onDropTrend);
        inspection.subscribe(CategoryReport.TYPE, archive::onReport);

        feed.setPrice("P1", money("450.00"));
        feed.setPrice("P1", money("400.00"));
        feed.setPrice("P1", money("390.00"));

        assertEquals(1, archive.reports().size());
        CategoryReport report = archive.reports().get(0);
        assertEquals("phones", report.categoryId());
        assertEquals(money("60.00"), report.taxTotal());
        assertEquals(List.of(), report.restockSkus());
    }
}
