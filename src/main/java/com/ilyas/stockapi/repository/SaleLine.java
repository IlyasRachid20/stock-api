package com.ilyas.stockapi.repository;

import java.math.BigDecimal;
import java.time.Instant;

/** One sold line with what the reports need, loaded in a single query (see SaleItemRepository). */
public record SaleLine(
        Long saleId,
        Instant saleDate,
        String customerName,
        Long productId,
        String productName,
        Long categoryId,
        String categoryName,
        Integer quantity,
        BigDecimal unitPrice) {

    public BigDecimal lineTotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
