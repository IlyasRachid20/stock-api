package com.ilyas.stockapi.repository;

import com.ilyas.stockapi.entity.MovementType;
import com.ilyas.stockapi.entity.StockMovement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {

    // Each filter is optional: null means "any"
    @Query("select m from StockMovement m where (:productId is null or m.product.id = :productId)"
            + " and (:type is null or m.type = :type)")
    Page<StockMovement> search(Long productId, MovementType type, Pageable pageable);

    // Loads the rows and removes them one by one (not a bulk query), so Hibernate's
    // in-memory state stays consistent with the database
    void deleteByProductId(Long productId);
}
