package com.marketplace.catalog;

/**
 * One overload per concrete element. A client holding a {@code CatalogNode} cannot call
 * {@code visit(node)} directly: overloads are chosen at compile time and there is no
 * {@code visit(CatalogNode)}. It calls {@code node.accept(visitor)} instead.
 */
public interface CatalogVisitor<R> {
    R visit(Catalog catalog);

    R visit(Category category);

    R visit(PhysicalProduct product);

    R visit(DigitalProduct product);

    R visit(ServiceProduct product);

    R visit(GiftCard giftCard);
}
