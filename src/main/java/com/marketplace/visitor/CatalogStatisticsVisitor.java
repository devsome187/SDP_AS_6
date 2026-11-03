package com.marketplace.visitor;

import com.marketplace.catalog.Catalog;
import com.marketplace.catalog.CatalogNode;
import com.marketplace.catalog.CatalogVisitor;
import com.marketplace.catalog.Category;
import com.marketplace.catalog.DigitalProduct;
import com.marketplace.catalog.GiftCard;
import com.marketplace.catalog.PhysicalProduct;
import com.marketplace.catalog.Product;
import com.marketplace.catalog.ServiceProduct;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Accumulates state across the whole tree in one pass: counts, current and maximum depth,
 * stock and the most expensive product. Instances are single-use and not thread-safe, so
 * the only entry point is {@link #collect}, which creates a fresh visitor per traversal.
 */
public final class CatalogStatisticsVisitor implements CatalogVisitor<Void> {
    private final Map<Class<? extends Product>, Integer> productsByType = new HashMap<>();
    private int categoryCount;
    private int depth;
    private int maxDepth;
    private int totalPhysicalStock;
    private Product mostExpensive;

    private CatalogStatisticsVisitor() {
    }

    public static CatalogStatistics collect(CatalogNode root) {
        CatalogStatisticsVisitor visitor = new CatalogStatisticsVisitor();
        root.accept(visitor);
        return new CatalogStatistics(visitor.categoryCount, visitor.productsByType, visitor.maxDepth,
                visitor.totalPhysicalStock, visitor.mostExpensive == null ? null : visitor.mostExpensive.sku());
    }

    @Override
    public Void visit(Catalog catalog) {
        visitChildren(catalog.categories());
        return null;
    }

    @Override
    public Void visit(Category category) {
        categoryCount++;
        visitChildren(category.entries());
        return null;
    }

    @Override
    public Void visit(PhysicalProduct product) {
        totalPhysicalStock += product.stock();
        countProduct(product);
        return null;
    }

    @Override
    public Void visit(DigitalProduct product) {
        countProduct(product);
        return null;
    }

    @Override
    public Void visit(ServiceProduct product) {
        countProduct(product);
        return null;
    }

    @Override
    public Void visit(GiftCard giftCard) {
        countProduct(giftCard);
        return null;
    }

    private void visitChildren(List<? extends CatalogNode> children) {
        depth++;
        if (!children.isEmpty()) {
            maxDepth = Math.max(maxDepth, depth);
        }
        children.forEach(child -> child.accept(this));
        depth--;
    }

    private void countProduct(Product product) {
        productsByType.merge(product.getClass(), 1, Integer::sum);
        if (mostExpensive == null || product.price().compareTo(mostExpensive.price()) > 0) {
            mostExpensive = product;
        }
    }
}
