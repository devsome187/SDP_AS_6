package com.marketplace.catalog;

import java.util.List;

/** Root container. */
public record Catalog(String name, List<Category> categories) implements CatalogNode {
    public Catalog {
        categories = List.copyOf(categories);
    }

    @Override
    public <R> R accept(CatalogVisitor<R> visitor) {
        return visitor.visit(this);
    }
}
