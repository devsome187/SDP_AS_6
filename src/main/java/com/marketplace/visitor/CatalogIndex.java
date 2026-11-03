package com.marketplace.visitor;

import com.marketplace.catalog.Catalog;
import com.marketplace.catalog.CatalogVisitor;
import com.marketplace.catalog.Category;
import com.marketplace.catalog.DigitalProduct;
import com.marketplace.catalog.GiftCard;
import com.marketplace.catalog.PhysicalProduct;
import com.marketplace.catalog.Product;
import com.marketplace.catalog.ServiceProduct;

import java.util.HashMap;
import java.util.Map;

/**
 * Lookup tables built once from the immutable catalog: categories by id and the id of the
 * category that directly contains each SKU. Read-only after construction, so safe to share.
 * This is how an event about one SKU finds the subtree a visitor should run on.
 */
public final class CatalogIndex {
    private final Map<String, Category> categoriesById;
    private final Map<String, String> categoryIdBySku;

    private CatalogIndex(Map<String, Category> categoriesById, Map<String, String> categoryIdBySku) {
        this.categoriesById = Map.copyOf(categoriesById);
        this.categoryIdBySku = Map.copyOf(categoryIdBySku);
    }

    public static CatalogIndex of(Catalog catalog) {
        IndexingVisitor visitor = new IndexingVisitor();
        catalog.accept(visitor);
        return new CatalogIndex(visitor.categoriesById, visitor.categoryIdBySku);
    }

    public Category category(String categoryId) {
        Category category = categoriesById.get(categoryId);
        if (category == null) {
            throw new IllegalArgumentException("Unknown category: " + categoryId);
        }
        return category;
    }

    public String categoryIdOf(String sku) {
        String categoryId = categoryIdBySku.get(sku);
        if (categoryId == null) {
            throw new IllegalArgumentException("Unknown SKU: " + sku);
        }
        return categoryId;
    }

    private static final class IndexingVisitor implements CatalogVisitor<Void> {
        private final Map<String, Category> categoriesById = new HashMap<>();
        private final Map<String, String> categoryIdBySku = new HashMap<>();
        private String currentCategoryId;

        @Override
        public Void visit(Catalog catalog) {
            catalog.categories().forEach(category -> category.accept(this));
            return null;
        }

        @Override
        public Void visit(Category category) {
            categoriesById.put(category.id(), category);
            String parentId = currentCategoryId;
            currentCategoryId = category.id();
            category.entries().forEach(entry -> entry.accept(this));
            currentCategoryId = parentId;
            return null;
        }

        @Override
        public Void visit(PhysicalProduct product) {
            return register(product);
        }

        @Override
        public Void visit(DigitalProduct product) {
            return register(product);
        }

        @Override
        public Void visit(ServiceProduct product) {
            return register(product);
        }

        @Override
        public Void visit(GiftCard giftCard) {
            return register(giftCard);
        }

        private Void register(Product product) {
            categoryIdBySku.put(product.sku(), currentCategoryId);
            return null;
        }
    }
}
