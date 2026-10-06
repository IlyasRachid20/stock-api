package com.ilyas.stockapi.dto;

import com.ilyas.stockapi.entity.PriceChange;
import java.math.BigDecimal;
import java.time.Instant;

public record PriceChangeResponse(Long id, BigDecimal oldPrice, BigDecimal newPrice, String changedBy, Instant changedAt) {

    public static PriceChangeResponse from(PriceChange change) {
        return new PriceChangeResponse(change.getId(), change.getOldPrice(), change.getNewPrice(),
                change.getChangedBy(), change.getChangedAt());
    }
}
