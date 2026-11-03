package com.marketplace.visitor;

import com.marketplace.catalog.Product;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * @param maxDepth depth of the deepest node, the visited root being depth 0
 */
public record CatalogStatistics(
        int categoryCount,
        Map<Class<? extends Product>, Integer> productsByType,
        int maxDepth,
        int totalPhysicalStock,
        String mostExpensiveSku) {

    public CatalogStatistics {
        productsByType = Map.copyOf(productsByType);
    }

    public int productCount() {
        return productsByType.values().stream().mapToInt(Integer::intValue).sum();
    }

    /** Readable summary, e.g. {@code 6 categories, 13 products (PhysicalProduct=5, ...), depth 3, ...}. */
    @Override
    public String toString() {
        String types = productsByType.entrySet().stream()
                .map(entry -> entry.getKey().getSimpleName() + "=" + entry.getValue())
                .sorted()
                .collect(Collectors.joining(", "));
        return categoryCount + " categories, " + productCount() + " products (" + types + "), depth " + maxDepth
                + ", physical stock " + totalPhysicalStock + ", most expensive " + mostExpensiveSku;
    }
}
