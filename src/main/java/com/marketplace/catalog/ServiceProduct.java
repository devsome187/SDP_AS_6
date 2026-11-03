package com.marketplace.catalog;

import java.math.BigDecimal;

public record ServiceProduct(String sku, String name, BigDecimal price, int durationHours) implements Product {

    @Override
    public <R> R accept(CatalogVisitor<R> visitor) {
        return visitor.visit(this);
    }
}
