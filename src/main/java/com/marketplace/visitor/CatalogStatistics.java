package com.marketplace.visitor;

import com.marketplace.catalog.Product;

import java.util.Map;

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
}
