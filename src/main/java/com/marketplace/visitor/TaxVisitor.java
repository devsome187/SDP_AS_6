package com.marketplace.visitor;

import com.marketplace.catalog.DigitalProduct;
import com.marketplace.catalog.GiftCard;
import com.marketplace.catalog.PhysicalProduct;
import com.marketplace.catalog.ServiceProduct;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Sales tax on one unit of every product. Goods (physical and digital) and services have
 * their own rates; gift cards are not taxed at sale. Each line is rounded half-up to cents.
 */
public final class TaxVisitor extends ProductSumVisitor {
    private final BigDecimal goodsRate;
    private final BigDecimal serviceRate;

    public TaxVisitor(BigDecimal goodsRate, BigDecimal serviceRate) {
        this.goodsRate = goodsRate;
        this.serviceRate = serviceRate;
    }

    @Override
    public BigDecimal visit(PhysicalProduct product) {
        return taxOf(product.price(), goodsRate);
    }

    @Override
    public BigDecimal visit(DigitalProduct product) {
        return taxOf(product.price(), goodsRate);
    }

    @Override
    public BigDecimal visit(ServiceProduct product) {
        return taxOf(product.price(), serviceRate);
    }

    @Override
    public BigDecimal visit(GiftCard giftCard) {
        return BigDecimal.ZERO;
    }

    private static BigDecimal taxOf(BigDecimal price, BigDecimal rate) {
        return price.multiply(rate).setScale(2, RoundingMode.HALF_UP);
    }
}
