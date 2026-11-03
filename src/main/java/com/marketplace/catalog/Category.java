package com.marketplace.catalog;

import java.util.List;

/** Nested container: holds sub-categories and products. */
public record Category(String id, String name, List<CategoryEntry> entries) implements CategoryEntry {
    public Category {
        entries = List.copyOf(entries);
    }

    @Override
    public <R> R accept(CatalogVisitor<R> visitor) {
        return visitor.visit(this);
    }
}
