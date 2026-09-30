package com.ilyas.stockapi.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

/**
 * Body of POST /api/sales. With items, the sale and all its items are saved in one transaction:
 * if any product is short of stock, nothing is saved. Without items, an empty sale is created and
 * items are added afterwards through /api/sale-items.
 */
public record SaleRequest(
        @NotNull Long customerId,
        @Size(max = 100) List<@Valid Item> items) {

    public SaleRequest(Long customerId) {
        this(customerId, null);
    }

    /** unitPrice is optional and defaults to the product's current price. */
    public record Item(
            @NotNull Long productId,
            @NotNull @Min(1) Integer quantity,
            @DecimalMin("0.00") @Digits(integer = 8, fraction = 2) BigDecimal unitPrice) {
    }
}
