package com.ilyas.stockapi.dto.report;

import java.math.BigDecimal;
import java.time.LocalDate;

/** One day of sales. Days without sales are included with zeros, so charts have no gaps. */
public record DailySales(LocalDate date, long salesCount, long itemsSold, BigDecimal revenue) {
}
