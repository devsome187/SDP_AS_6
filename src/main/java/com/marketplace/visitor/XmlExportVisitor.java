package com.marketplace.visitor;

import com.marketplace.catalog.Catalog;
import com.marketplace.catalog.CatalogNode;
import com.marketplace.catalog.CatalogVisitor;
import com.marketplace.catalog.Category;
import com.marketplace.catalog.DigitalProduct;
import com.marketplace.catalog.GiftCard;
import com.marketplace.catalog.PhysicalProduct;
import com.marketplace.catalog.ServiceProduct;
import com.marketplace.catalog.SubscriptionProduct;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Hand-written XML export. Every node returns its own fragment; a container wraps its
 * children's fragments, indented one level, between its opening and closing tags, so
 * nesting is correct by construction and the visitor keeps no mutable state.
 */
public final class XmlExportVisitor implements CatalogVisitor<String> {
    private static final String INDENT = "  ";

    @Override
    public String visit(Catalog catalog) {
        return element("catalog", attribute("name", catalog.name()), catalog.categories());
    }

    @Override
    public String visit(Category category) {
        return element("category", attribute("id", category.id()) + attribute("name", category.name()),
                category.entries());
    }

    @Override
    public String visit(PhysicalProduct product) {
        return emptyElement("physical", productAttributes(product.sku(), product.name(), product.price().toPlainString())
                + attribute("weightKg", product.weightKg().toPlainString())
                + attribute("stock", String.valueOf(product.stock())));
    }

    @Override
    public String visit(DigitalProduct product) {
        return emptyElement("digital", productAttributes(product.sku(), product.name(), product.price().toPlainString())
                + attribute("downloadSizeMb", String.valueOf(product.downloadSizeMb())));
    }

    @Override
    public String visit(ServiceProduct product) {
        return emptyElement("service", productAttributes(product.sku(), product.name(), product.price().toPlainString())
                + attribute("durationHours", String.valueOf(product.durationHours())));
    }

    @Override
    public String visit(GiftCard giftCard) {
        return emptyElement("giftCard", productAttributes(giftCard.sku(), giftCard.name(), giftCard.price().toPlainString()));
    }

    @Override
    public String visit(SubscriptionProduct subscription) {
        return emptyElement("subscription", productAttributes(subscription.sku(), subscription.name(), subscription.price().toPlainString())
                + attribute("billingMonths", String.valueOf(subscription.billingMonths())));
    }

    private String element(String tag, String attributes, List<? extends CatalogNode> children) {
        if (children.isEmpty()) {
            return emptyElement(tag, attributes);
        }
        String body = children.stream()
                .map(child -> child.accept(this))
                .map(XmlExportVisitor::indent)
                .collect(Collectors.joining("\n"));
        return "<" + tag + attributes + ">\n" + body + "\n</" + tag + ">";
    }

    private static String emptyElement(String tag, String attributes) {
        return "<" + tag + attributes + "/>";
    }

    private static String productAttributes(String sku, String name, String price) {
        return attribute("sku", sku) + attribute("name", name) + attribute("price", price);
    }

    private static String attribute(String name, String value) {
        return " " + name + "=\"" + escape(value) + "\"";
    }

    private static String indent(String fragment) {
        return fragment.lines().map(line -> INDENT + line).collect(Collectors.joining("\n"));
    }

    static String escape(String value) {
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}
