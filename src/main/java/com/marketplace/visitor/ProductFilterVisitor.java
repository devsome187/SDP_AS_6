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

import java.util.List;
import java.util.function.Predicate;

/** Returns the products that match the predicate, in depth-first catalog order. */
public final class ProductFilterVisitor implements CatalogVisitor<List<Product>> {
    private final Predicate<Product> predicate;

    public ProductFilterVisitor(Predicate<Product> predicate) {
        this.predicate = predicate;
    }

    @Override
    public List<Product> visit(Catalog catalog) {
        return collect(catalog.categories());
    }

    @Override
    public List<Product> visit(Category category) {
        return collect(category.entries());
    }

    @Override
    public List<Product> visit(PhysicalProduct product) {
        return test(product);
    }

    @Override
    public List<Product> visit(DigitalProduct product) {
        return test(product);
    }

    @Override
    public List<Product> visit(ServiceProduct product) {
        return test(product);
    }

    @Override
    public List<Product> visit(GiftCard giftCard) {
        return test(giftCard);
    }

    private List<Product> collect(List<? extends CatalogNode> children) {
        return children.stream()
                .flatMap(child -> child.accept(this).stream())
                .toList();
    }

    private List<Product> test(Product product) {
        return predicate.test(product) ? List.of(product) : List.of();
    }
}
