package com.ilyas.stockapi.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * Body of POST and PUT /api/products.
 * quantity and minQuantity are optional: 0 when creating, unchanged when updating.
 * Changing quantity here is recorded as an ADJUSTMENT; prefer the restock and adjustments endpoints.
 */
public record ProductRequest(
        @NotBlank @Size(max = 150) String name,
        @NotNull @DecimalMin("0.00") @Digits(integer = 8, fraction = 2) BigDecimal price,
        @Min(0) Integer quantity,
        @Min(0) Integer minQuantity) {

    public ProductRequest {
        name = (name == null) ? null : name.trim();
    }
}
