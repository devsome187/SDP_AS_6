package com.marketplace.listener;

import com.marketplace.event.PriceChanged;

import java.math.BigDecimal;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/** Raises an alert for every single price drop at or above the configured percentage. */
public final class PriceDropAlert {
    private final BigDecimal thresholdPercent;
    private final Queue<PriceChanged> alerts = new ConcurrentLinkedQueue<>();

    public PriceDropAlert(BigDecimal thresholdPercent) {
        this.thresholdPercent = thresholdPercent;
    }

    public void onPriceChanged(PriceChanged event) {
        if (event.isDrop() && event.dropPercent().compareTo(thresholdPercent) >= 0) {
            alerts.add(event);
        }
    }

    public List<PriceChanged> alerts() {
        return List.copyOf(alerts);
    }
}
