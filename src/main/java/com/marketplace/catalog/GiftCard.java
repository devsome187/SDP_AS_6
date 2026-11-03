package com.marketplace.catalog;

import java.math.BigDecimal;

/** A plastic gift card mailed to the buyer; {@code price} is its face value. */
public record GiftCard(String sku, String name, BigDecimal price) implements Product {

    @Override
    public <R> R accept(CatalogVisitor<R> visitor) {
        return visitor.visit(this);
    }
}
