package com.marketplace.catalog;

/**
 * Element of the catalog Composite. {@code accept} is the first dispatch (on the node's
 * runtime class); inside it {@code visitor.visit(this)} is the second one, resolved by the
 * compiler because {@code this} has a concrete static type there.
 */
public sealed interface CatalogNode permits Catalog, CategoryEntry {
    <R> R accept(CatalogVisitor<R> visitor);
}
