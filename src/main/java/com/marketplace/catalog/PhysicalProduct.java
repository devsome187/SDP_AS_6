package com.marketplace.catalog;

import java.math.BigDecimal;

public record PhysicalProduct(String sku, String name, BigDecimal price, BigDecimal weightKg, int stock)
        implements Product {

    @Override
    public <R> R accept(CatalogVisitor<R> visitor) {
        return visitor.visit(this);
    }
}
