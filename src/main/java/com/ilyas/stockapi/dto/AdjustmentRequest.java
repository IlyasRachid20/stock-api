package com.ilyas.stockapi.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Body of POST /api/products/{id}/adjustments: a correction after a count, damage or loss.
 * quantityChange is negative to remove units (-2) and positive to add them (+1). A reason is required,
 * so every correction can be explained later.
 */
public record AdjustmentRequest(
        @NotNull @Min(-1_000_000) @Max(1_000_000) Integer quantityChange,
        @NotBlank @Size(max = 255) String reason) {

    @AssertTrue(message = "must not be 0")
    public boolean isQuantityChangeNotZero() {
        return quantityChange == null || quantityChange != 0;
    }
}
