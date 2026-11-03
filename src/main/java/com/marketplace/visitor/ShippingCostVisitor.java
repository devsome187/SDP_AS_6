package com.marketplace.visitor;

import com.marketplace.catalog.DigitalProduct;
import com.marketplace.catalog.GiftCard;
import com.marketplace.catalog.PhysicalProduct;
import com.marketplace.catalog.ServiceProduct;
import com.marketplace.catalog.SubscriptionProduct;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Cost of shipping one unit of every product: parcels pay a base fee plus a per-kg fee,
 * gift cards go by letter at a flat fee, digital goods, services and subscriptions ship for free.
 */
public final class ShippingCostVisitor extends ProductSumVisitor {
    private final BigDecimal parcelBaseFee;
    private final BigDecimal perKgFee;
    private final BigDecimal letterFee;

    public ShippingCostVisitor(BigDecimal parcelBaseFee, BigDecimal perKgFee, BigDecimal letterFee) {
        this.parcelBaseFee = parcelBaseFee;
        this.perKgFee = perKgFee;
        this.letterFee = letterFee;
    }

    @Override
    public BigDecimal visit(PhysicalProduct product) {
        return parcelBaseFee.add(perKgFee.multiply(product.weightKg())).setScale(2, RoundingMode.HALF_UP);
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
        return letterFee;
    }

    @Override
    public BigDecimal visit(SubscriptionProduct subscription) {
        return BigDecimal.ZERO;
    }
}
