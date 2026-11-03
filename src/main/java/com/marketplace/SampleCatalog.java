package com.marketplace;

import com.marketplace.catalog.Catalog;
import com.marketplace.catalog.Category;
import com.marketplace.catalog.DigitalProduct;
import com.marketplace.catalog.GiftCard;
import com.marketplace.catalog.PhysicalProduct;
import com.marketplace.catalog.ServiceProduct;
import com.marketplace.catalog.SubscriptionProduct;

import java.math.BigDecimal;
import java.util.List;

/** Demo data for the application: Catalog → Category → Category → products. */
final class SampleCatalog {
    private SampleCatalog() {
    }

    static Catalog create() {
        Category laptops = new Category("laptops", "Laptops", List.of(
                new PhysicalProduct("LAP-001", "UltraBook 14", money("1200.00"), new BigDecimal("1.3"), 8),
                new PhysicalProduct("LAP-002", "Gamer 17", money("1850.00"), new BigDecimal("3.1"), 3),
                new DigitalProduct("SW-001", "Office Suite", money("99.00"), 2400)));
        Category phones = new Category("phones", "Phones", List.of(
                new PhysicalProduct("PH-001", "Pocket 6", money("640.00"), new BigDecimal("0.2"), 25),
                new PhysicalProduct("PH-002", "Fold & Flip", money("1400.00"), new BigDecimal("0.3"), 4)));
        Category electronics = new Category("electronics", "Electronics", List.of(laptops, phones));

        Category ebooks = new Category("ebooks", "E-books", List.of(
                new DigitalProduct("EB-001", "Clean Code", money("25.00"), 6),
                new DigitalProduct("EB-002", "Design Patterns", money("30.00"), 9)));
        Category books = new Category("books", "Books", List.of(
                ebooks,
                new PhysicalProduct("BK-001", "Refactoring (hardcover)", money("45.00"), new BigDecimal("0.9"), 12)));

        Category services = new Category("services", "Services & Gifts", List.of(
                new ServiceProduct("SV-001", "Laptop setup", money("40.00"), 1),
                new ServiceProduct("SV-002", "Extended warranty", money("150.00"), 0),
                new GiftCard("GC-050", "Gift card 50", money("50.00")),
                new GiftCard("GC-100", "Gift card 100", money("100.00")),
                new SubscriptionProduct("SUB-001", "Cloud backup", money("9.99"), 1)));

        return new Catalog("Online Marketplace", List.of(electronics, books, services));
    }

    private static BigDecimal money(String amount) {
        return new BigDecimal(amount);
    }
}
