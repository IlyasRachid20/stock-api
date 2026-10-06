package com.ilyas.stockapi.dto;

import com.ilyas.stockapi.entity.Product;
import java.math.BigDecimal;
import java.time.Instant;

/** previousPrice is the struck-through price during the 30 days after a price drop, null otherwise. */
public record ProductResponse(Long id, String name, CategorySummary category, BigDecimal price,
                              BigDecimal previousPrice, Integer quantity, Integer minQuantity, boolean lowStock) {

    public static ProductResponse from(Product product) {
        return new ProductResponse(product.getId(), product.getName(), CategorySummary.from(product.getCategory()),
                product.getPrice(), product.getPreviousPriceAt(Instant.now()), product.getQuantity(),
                product.getMinQuantity(), product.isLowStock());
    }
}
