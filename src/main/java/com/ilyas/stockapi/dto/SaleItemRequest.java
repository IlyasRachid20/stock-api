package com.ilyas.stockapi.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/** Body of POST /api/sale-items. unitPrice is optional and defaults to the product's current price. */
public record SaleItemRequest(
        @NotNull Long saleId,
        @NotNull Long productId,
        @NotNull @Min(1) Integer quantity,
        @DecimalMin("0.00") @Digits(integer = 8, fraction = 2) BigDecimal unitPrice) {
}
