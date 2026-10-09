package com.ilyas.stockapi.dto.report;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Totals for a date range (from and to included, in the shop's time zone). The online figures are
 * the part of the totals that came from delivered online orders.
 */
public record SalesSummary(
        LocalDate from,
        LocalDate to,
        long salesCount,
        long itemsSold,
        BigDecimal revenue,
        BigDecimal averageSale,
        long onlineSalesCount,
        BigDecimal onlineRevenue) {
}
