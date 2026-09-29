package com.ilyas.stockapi.dto;

import com.ilyas.stockapi.entity.Product;
import java.math.BigDecimal;

public record ProductResponse(Long id, String name, BigDecimal price, Integer quantity,
                              Integer minQuantity, boolean lowStock) {

    public static ProductResponse from(Product product) {
        return new ProductResponse(product.getId(), product.getName(), product.getPrice(), product.getQuantity(),
                product.getMinQuantity(), product.isLowStock());
    }
}
