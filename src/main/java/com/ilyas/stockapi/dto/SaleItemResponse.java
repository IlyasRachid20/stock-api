package com.ilyas.stockapi.dto;

import com.ilyas.stockapi.entity.SaleItem;
import java.math.BigDecimal;

public record SaleItemResponse(
        Long id,
        Long saleId,
        ProductSummary product,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal lineTotal) {

    public static SaleItemResponse from(SaleItem item) {
        return new SaleItemResponse(
                item.getId(),
                item.getSale().getId(),
                ProductSummary.from(item.getProduct()),
                item.getQuantity(),
                item.getUnitPrice(),
                item.getLineTotal());
    }
}
