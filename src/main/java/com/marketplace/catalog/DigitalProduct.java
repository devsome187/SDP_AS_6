package com.marketplace.catalog;

import java.math.BigDecimal;

public record DigitalProduct(String sku, String name, BigDecimal price, int downloadSizeMb) implements Product {

    @Override
    public <R> R accept(CatalogVisitor<R> visitor) {
        return visitor.visit(this);
    }
}
