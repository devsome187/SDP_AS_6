package com.marketplace.concurrency;

import com.marketplace.feed.InventoryFeed;
import com.marketplace.feed.PriceFeed;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Random;

/**
 * Producer task: emits exactly {@code eventCount} events, each either a price change of
 * -10%..+10% or a stock adjustment of -3..+3. Seeded, so a run can be reproduced.
 */
public final class MarketTrafficGenerator implements Runnable {
    private static final BigDecimal MIN_PRICE = new BigDecimal("0.01");

    private final PriceFeed priceFeed;
    private final InventoryFeed inventoryFeed;
    private final List<String> pricedSkus;
    private final List<String> stockedSkus;
    private final int eventCount;
    private final long seed;

    public MarketTrafficGenerator(PriceFeed priceFeed, InventoryFeed inventoryFeed,
                                  List<String> pricedSkus, List<String> stockedSkus, int eventCount, long seed) {
        this.priceFeed = priceFeed;
        this.inventoryFeed = inventoryFeed;
        this.pricedSkus = List.copyOf(pricedSkus);
        this.stockedSkus = List.copyOf(stockedSkus);
        this.eventCount = eventCount;
        this.seed = seed;
    }

    @Override
    public void run() {
        Random random = new Random(seed);
        for (int i = 0; i < eventCount; i++) {
            if (stockedSkus.isEmpty() || random.nextInt(10) < 6) {
                BigDecimal factor = BigDecimal.valueOf(90 + random.nextInt(21), 2);
                priceFeed.reprice(pick(pricedSkus, random), price -> applyFactor(price, factor));
            } else {
                inventoryFeed.adjustStock(pick(stockedSkus, random), random.nextInt(7) - 3);
            }
        }
    }

    private static String pick(List<String> skus, Random random) {
        return skus.get(random.nextInt(skus.size()));
    }

    private static BigDecimal applyFactor(BigDecimal price, BigDecimal factor) {
        return price.multiply(factor).setScale(2, RoundingMode.HALF_UP).max(MIN_PRICE);
    }
}
