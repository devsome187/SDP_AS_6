package com.marketplace.visitor;

import com.marketplace.catalog.CatalogNode;
import com.marketplace.catalog.DigitalProduct;
import com.marketplace.catalog.GiftCard;
import com.marketplace.catalog.PhysicalProduct;
import com.marketplace.catalog.ServiceProduct;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static com.marketplace.TestCatalog.CATALOG;
import static com.marketplace.TestCatalog.D1;
import static com.marketplace.TestCatalog.ELECTRONICS;
import static com.marketplace.TestCatalog.G1;
import static com.marketplace.TestCatalog.L1;
import static com.marketplace.TestCatalog.LAPTOPS;
import static com.marketplace.TestCatalog.S1;
import static com.marketplace.TestCatalog.money;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class VisitorTest {
    private final TaxVisitor tax = new TaxVisitor(new BigDecimal("0.12"), new BigDecimal("0.05"));
    private final ShippingCostVisitor shipping = new ShippingCostVisitor(money("5.00"), money("2.00"), money("1.50"));

    @Test
    void taxMatchesHandCalculation() {
        // L1 1000*0.12=120.00, D1 100*0.12=12.00, P1 500*0.12=60.00, S1 50*0.05=2.50, G1 0
        assertEquals(money("194.50"), CATALOG.accept(tax));
        assertEquals(money("192.00"), ELECTRONICS.accept(tax));
    }

    @Test
    void shippingMatchesHandCalculation() {
        // L1 5+2*2.0=9.00, P1 5+2*0.5=6.00, G1 letter 1.50, D1 and S1 free
        assertEquals(money("16.50"), CATALOG.accept(shipping));
        assertEquals(money("9.00"), LAPTOPS.accept(shipping));
    }

    @Test
    void statisticsAccumulateAcrossTheWholeTree() {
        CatalogStatistics statistics = CatalogStatisticsVisitor.collect(CATALOG);

        assertEquals(4, statistics.categoryCount());
        assertEquals(5, statistics.productCount());
        assertEquals(Map.of(PhysicalProduct.class, 2, DigitalProduct.class, 1,
                ServiceProduct.class, 1, GiftCard.class, 1), statistics.productsByType());
        assertEquals(3, statistics.maxDepth());
        assertEquals(15, statistics.totalPhysicalStock());
        assertEquals("L1", statistics.mostExpensiveSku());
    }

    @Test
    void filterReturnsMatchingProductsInCatalogOrder() {
        ProductFilterVisitor cheap = new ProductFilterVisitor(product -> product.price().compareTo(money("200.00")) < 0);

        assertEquals(List.of(D1, S1, G1), CATALOG.accept(cheap));
        assertEquals(List.of(D1), LAPTOPS.accept(cheap));
    }

    @Test
    void xmlExportNestsAndEscapesCorrectly() {
        String expected = String.join("\n",
                "<catalog name=\"Test\">",
                "  <category id=\"electronics\" name=\"Electronics\">",
                "    <category id=\"laptops\" name=\"Laptops\">",
                "      <physical sku=\"L1\" name=\"Laptop &amp; &quot;Pro&quot;\" price=\"1000.00\" weightKg=\"2.0\" stock=\"5\"/>",
                "      <digital sku=\"D1\" name=\"Office\" price=\"100.00\" downloadSizeMb=\"500\"/>",
                "    </category>",
                "    <category id=\"phones\" name=\"Phones\">",
                "      <physical sku=\"P1\" name=\"Phone\" price=\"500.00\" weightKg=\"0.5\" stock=\"10\"/>",
                "    </category>",
                "  </category>",
                "  <category id=\"services\" name=\"Services\">",
                "    <service sku=\"S1\" name=\"Setup\" price=\"50.00\" durationHours=\"2\"/>",
                "    <giftCard sku=\"G1\" name=\"Gift &lt;100&gt;\" price=\"100.00\"/>",
                "  </category>",
                "</catalog>");

        assertEquals(expected, CATALOG.accept(new XmlExportVisitor()));
    }

    @Test
    void acceptDispatchesOnTheRuntimeTypeOfAStaticallyGenericNode() {
        CatalogNode node = L1; // static type is the interface, runtime type is PhysicalProduct

        assertEquals(money("120.00"), node.accept(tax));
        assertEquals(money("9.00"), node.accept(shipping));
    }

    @Test
    void indexMapsSkusToTheirDirectCategory() {
        CatalogIndex index = CatalogIndex.of(CATALOG);

        assertEquals("laptops", index.categoryIdOf("D1"));
        assertEquals("services", index.categoryIdOf("G1"));
        assertEquals(LAPTOPS, index.category("laptops"));
        assertThrows(IllegalArgumentException.class, () -> index.category("missing"));
    }
}
