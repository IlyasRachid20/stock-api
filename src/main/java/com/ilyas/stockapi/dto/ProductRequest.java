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
 * quantity is optional: 0 when creating, unchanged when updating.
 */
public record ProductRequest(
        @NotBlank @Size(max = 150) String name,
        @NotNull @DecimalMin("0.00") @Digits(integer = 8, fraction = 2) BigDecimal price,
        @Min(0) Integer quantity) {
}
