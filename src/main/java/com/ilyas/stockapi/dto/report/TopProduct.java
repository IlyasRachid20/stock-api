package com.ilyas.stockapi.dto.report;

import java.math.BigDecimal;

public record TopProduct(Long productId, String name, long quantitySold, BigDecimal revenue) {
}
