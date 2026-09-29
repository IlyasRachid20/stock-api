package com.ilyas.stockapi.dto;

import com.ilyas.stockapi.entity.Product;

/** Short product info shown inside a sale item. */
public record ProductSummary(Long id, String name) {

    public static ProductSummary from(Product product) {
        return new ProductSummary(product.getId(), product.getName());
    }
}
