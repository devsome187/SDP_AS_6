package com.marketplace.catalog;

/** What may live inside a category: a sub-category or a product, but never a whole catalog. */
public sealed interface CategoryEntry extends CatalogNode permits Category, Product {
}
