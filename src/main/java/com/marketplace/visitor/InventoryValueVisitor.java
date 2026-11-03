package com.marketplace.visitor;

import com.marketplace.catalog.DigitalProduct;
import com.marketplace.catalog.GiftCard;
import com.marketplace.catalog.PhysicalProduct;
import com.marketplace.catalog.ServiceProduct;

import java.math.BigDecimal;

/**
 * Value of goods sitting in the warehouse: price × stock of physical products.
 * Digital goods, services and gift cards are issued on demand and hold no inventory.
 */
public final class InventoryValueVisitor extends ProductSumVisitor {

    @Override
    public BigDecimal visit(PhysicalProduct product) {
        return product.price().multiply(BigDecimal.valueOf(product.stock()));
    }

    @Override
    public BigDecimal visit(DigitalProduct product) {
        return BigDecimal.ZERO;
    }

    @Override
    public BigDecimal visit(ServiceProduct product) {
        return BigDecimal.ZERO;
    }

    @Override
    public BigDecimal visit(GiftCard giftCard) {
        return BigDecimal.ZERO;
    }
}
