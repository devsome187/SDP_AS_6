package com.marketplace.visitor;

import com.marketplace.catalog.Catalog;
import com.marketplace.catalog.CatalogNode;
import com.marketplace.catalog.CatalogVisitor;
import com.marketplace.catalog.Category;

import java.math.BigDecimal;
import java.util.List;

/**
 * Shared traversal for numeric visitors: a container's value is the sum of its children.
 * Subclasses only say what each kind of product contributes.
 */
public abstract class ProductSumVisitor implements CatalogVisitor<BigDecimal> {

    @Override
    public BigDecimal visit(Catalog catalog) {
        return sum(catalog.categories());
    }

    @Override
    public BigDecimal visit(Category category) {
        return sum(category.entries());
    }

    private BigDecimal sum(List<? extends CatalogNode> children) {
        return children.stream()
                .map(child -> child.accept(this))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
