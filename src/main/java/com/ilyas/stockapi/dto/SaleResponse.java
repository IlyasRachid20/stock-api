package com.ilyas.stockapi.dto;

import com.ilyas.stockapi.entity.Sale;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record SaleResponse(
        Long id,
        CustomerSummary customer,
        Instant saleDate,
        List<SaleItemResponse> items,
        BigDecimal total) {

    public static SaleResponse from(Sale sale) {
        return new SaleResponse(
                sale.getId(),
                CustomerSummary.from(sale.getCustomer()),
                sale.getSaleDate(),
                sale.getItems().stream().map(SaleItemResponse::from).toList(),
                sale.getTotal());
    }
}
