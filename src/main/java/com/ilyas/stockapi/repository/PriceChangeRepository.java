package com.ilyas.stockapi.repository;

import com.ilyas.stockapi.entity.PriceChange;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public interface PriceChangeRepository extends JpaRepository<PriceChange, Long> {

    // Newest first
    List<PriceChange> findByProductIdOrderByChangedAtDescIdDesc(Long productId);

    // Each change's old price was in effect until that change, so the lowest old price of the
    // changes made since a moment is the lowest price the product had since then
    @Query("select min(c.oldPrice) from PriceChange c where c.product.id = :productId and c.changedAt >= :since")
    BigDecimal findLowestOldPriceSince(Long productId, Instant since);

    // Loads the rows and removes them one by one (see StockMovementRepository.deleteByProductId)
    void deleteByProductId(Long productId);
}
