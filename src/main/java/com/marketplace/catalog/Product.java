package com.marketplace.catalog;

import java.math.BigDecimal;

/** Common data of every leaf, so filters and exports can treat products uniformly. */
public sealed interface Product extends CategoryEntry
        permits PhysicalProduct, DigitalProduct, ServiceProduct, GiftCard, SubscriptionProduct {
    String sku();

    String name();

    BigDecimal price();
}
