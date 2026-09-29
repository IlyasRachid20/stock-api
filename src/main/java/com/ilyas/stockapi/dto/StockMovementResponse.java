package com.ilyas.stockapi.dto;

import com.ilyas.stockapi.entity.MovementType;
import com.ilyas.stockapi.entity.StockMovement;
import java.time.Instant;

public record StockMovementResponse(
        Long id,
        ProductSummary product,
        MovementType type,
        Integer quantityChange,
        Integer quantityAfter,
        String reason,
        Long saleItemId,
        String createdBy,
        Instant createdAt) {

    public static StockMovementResponse from(StockMovement movement) {
        return new StockMovementResponse(
                movement.getId(),
                ProductSummary.from(movement.getProduct()),
                movement.getType(),
                movement.getQuantityChange(),
                movement.getQuantityAfter(),
                movement.getReason(),
                movement.getSaleItemId(),
                movement.getCreatedBy(),
                movement.getCreatedAt());
    }
}
