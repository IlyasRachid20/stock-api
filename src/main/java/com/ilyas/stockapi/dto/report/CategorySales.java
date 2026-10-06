package com.ilyas.stockapi.dto.report;

import java.math.BigDecimal;

/** Sales of one category over a date range; categoryId is null for products without a category. */
public record CategorySales(Long categoryId, String name, long quantitySold, BigDecimal revenue) {
}
