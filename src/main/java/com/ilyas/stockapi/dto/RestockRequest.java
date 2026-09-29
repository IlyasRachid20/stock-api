package com.ilyas.stockapi.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Body of POST /api/products/{id}/restock: goods received. */
public record RestockRequest(
        @NotNull @Min(1) @Max(1_000_000) Integer quantity,
        @Size(max = 255) String reason) {
}
