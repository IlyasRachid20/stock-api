package com.ilyas.stockapi.dto;

import com.ilyas.stockapi.entity.Sale;
import com.ilyas.stockapi.entity.SaleChannel;
import com.ilyas.stockapi.entity.SaleStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record SaleResponse(
        Long id,
        CustomerSummary customer,
        Instant saleDate,
        List<SaleItemResponse> items,
        BigDecimal total,
        SaleChannel channel,
        SaleStatus status,
        String orderNumber) {

    public static SaleResponse from(Sale sale) {
        return new SaleResponse(
                sale.getId(),
                CustomerSummary.from(sale.getCustomer()),
                sale.getSaleDate(),
                sale.getItems().stream().map(SaleItemResponse::from).toList(),
                sale.getTotal(),
                sale.getChannel(),
                sale.getStatus(),
                sale.getOrderNumber());
    }
}
