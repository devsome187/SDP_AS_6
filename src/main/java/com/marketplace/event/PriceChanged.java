package com.marketplace.event;

import com.marketplace.observer.Event;
import com.marketplace.observer.EventType;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * @param version per-SKU sequence number; lets listeners ignore events that arrive out of order
 */
public record PriceChanged(String sku, BigDecimal oldPrice, BigDecimal newPrice, long version) implements Event {
    public static final EventType<PriceChanged> TYPE = EventType.named("PRICE_CHANGED");

    @Override
    public EventType<PriceChanged> type() {
        return TYPE;
    }

    public boolean isDrop() {
        return newPrice.compareTo(oldPrice) < 0;
    }

    /** Drop relative to the old price in percent, e.g. 100.00 → 75.00 gives 25.00. Zero for rises. */
    public BigDecimal dropPercent() {
        if (!isDrop()) {
            return BigDecimal.ZERO;
        }
        return oldPrice.subtract(newPrice)
                .multiply(BigDecimal.valueOf(100))
                .divide(oldPrice, 2, RoundingMode.HALF_UP);
    }
}
