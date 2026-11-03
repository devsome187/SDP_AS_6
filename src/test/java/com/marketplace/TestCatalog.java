package com.marketplace;

import com.marketplace.catalog.Catalog;
import com.marketplace.catalog.Category;
import com.marketplace.catalog.DigitalProduct;
import com.marketplace.catalog.GiftCard;
import com.marketplace.catalog.PhysicalProduct;
import com.marketplace.catalog.ServiceProduct;

import java.math.BigDecimal;
import java.util.List;

/**
 * Small fixed tree used for hand-calculated expectations:
 * <pre>
 * Catalog "Test"
 * ├── electronics
 * │   ├── laptops : L1 physical 1000.00 2.0 kg stock 5 | D1 digital 100.00
 * │   └── phones  : P1 physical  500.00 0.5 kg stock 10
 * └── services    : S1 service 50.00 | G1 gift card 100.00
 * </pre>
 */
public final class TestCatalog {
    public static final PhysicalProduct L1 = new PhysicalProduct("L1", "Laptop & \"Pro\"", money("1000.00"), new BigDecimal("2.0"), 5);
    public static final DigitalProduct D1 = new DigitalProduct("D1", "Office", money("100.00"), 500);
    public static final PhysicalProduct P1 = new PhysicalProduct("P1", "Phone", money("500.00"), new BigDecimal("0.5"), 10);
    public static final ServiceProduct S1 = new ServiceProduct("S1", "Setup", money("50.00"), 2);
    public static final GiftCard G1 = new GiftCard("G1", "Gift <100>", money("100.00"));

    public static final Category LAPTOPS = new Category("laptops", "Laptops", List.of(L1, D1));
    public static final Category PHONES = new Category("phones", "Phones", List.of(P1));
    public static final Category ELECTRONICS = new Category("electronics", "Electronics", List.of(LAPTOPS, PHONES));
    public static final Category SERVICES = new Category("services", "Services", List.of(S1, G1));
    public static final Catalog CATALOG = new Catalog("Test", List.of(ELECTRONICS, SERVICES));

    private TestCatalog() {
    }

    public static BigDecimal money(String amount) {
        return new BigDecimal(amount);
    }
}
