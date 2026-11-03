package com.marketplace.catalog;

import java.math.BigDecimal;

/** A recurring plan, e.g. a streaming bundle; {@code price} is charged every billing period. */
public record SubscriptionProduct(String sku, String name, BigDecimal price, int billingMonths) implements Product {

    @Override
    public <R> R accept(CatalogVisitor<R> visitor) {
        return visitor.visit(this);
    }
}
