package com.marketplace.integration;

import com.marketplace.catalog.Category;
import com.marketplace.catalog.Product;
import com.marketplace.event.CategoryDropTrend;
import com.marketplace.event.CategoryReport;
import com.marketplace.observer.EventType;
import com.marketplace.observer.Observer;
import com.marketplace.observer.ObserverRegistry;
import com.marketplace.observer.Subject;
import com.marketplace.observer.Subscription;
import com.marketplace.visitor.CatalogIndex;
import com.marketplace.visitor.ProductFilterVisitor;
import com.marketplace.visitor.ShippingCostVisitor;
import com.marketplace.visitor.TaxVisitor;
import com.marketplace.visitor.XmlExportVisitor;

import java.util.List;

/**
 * The bridge between the two patterns: a {@link CategoryDropTrend} (Observer side) triggers
 * visitors on the affected category subtree only (Visitor side), and the outcome re-enters
 * the system as a {@link CategoryReport} event. All injected visitors are stateless, so one
 * instance serves concurrent trends from different producer threads.
 */
public final class CategoryInspectionService implements Subject<CategoryReport> {
    private final ObserverRegistry<CategoryReport> registry;
    private final CatalogIndex catalogIndex;
    private final TaxVisitor taxVisitor;
    private final ShippingCostVisitor shippingVisitor;
    private final ProductFilterVisitor restockFilter;
    private final XmlExportVisitor xmlExport;

    public CategoryInspectionService(ObserverRegistry<CategoryReport> registry, CatalogIndex catalogIndex,
                                     TaxVisitor taxVisitor, ShippingCostVisitor shippingVisitor,
                                     ProductFilterVisitor restockFilter, XmlExportVisitor xmlExport) {
        this.registry = registry;
        this.catalogIndex = catalogIndex;
        this.taxVisitor = taxVisitor;
        this.shippingVisitor = shippingVisitor;
        this.restockFilter = restockFilter;
        this.xmlExport = xmlExport;
    }

    @Override
    public <T extends CategoryReport> Subscription subscribe(EventType<T> type, Observer<? super T> observer) {
        return registry.subscribe(type, observer);
    }

    public void onDropTrend(CategoryDropTrend trend) {
        Category subtree = catalogIndex.category(trend.categoryId());
        List<String> restockSkus = subtree.accept(restockFilter).stream().map(Product::sku).toList();
        registry.publish(new CategoryReport(
                subtree.id(),
                trend.dropCount(),
                subtree.accept(taxVisitor),
                subtree.accept(shippingVisitor),
                restockSkus,
                subtree.accept(xmlExport)));
    }
}
