package com.ilyas.stockapi.dto;

import jakarta.validation.constraints.NotNull;

/** Body of POST /api/sales. Items are added afterwards through /api/sale-items. */
public record SaleRequest(@NotNull Long customerId) {
}
