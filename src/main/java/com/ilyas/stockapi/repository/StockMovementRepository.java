package com.ilyas.stockapi.repository;

import com.ilyas.stockapi.entity.MovementType;
import com.ilyas.stockapi.entity.StockMovement;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface StockMovementRepository extends JpaRepository<StockMovement, Long>, JpaSpecificationExecutor<StockMovement> {

    // Each filter is optional: null means "any". A Specification, so ?sort= fields are checked (see SearchText)
    static Specification<StockMovement> matching(Long productId, MovementType type) {
        return (root, query, cb) -> {
            List<Predicate> filters = new ArrayList<>();
            if (productId != null) {
                filters.add(cb.equal(root.get("product").get("id"), productId));
            }
            if (type != null) {
                filters.add(cb.equal(root.get("type"), type));
            }
            return cb.and(filters.toArray(Predicate[]::new));
        };
    }

    // Loads the rows and removes them one by one (not a bulk query), so Hibernate's
    // in-memory state stays consistent with the database
    void deleteByProductId(Long productId);

    List<StockMovement> findBySaleItemIdIn(Collection<Long> saleItemIds);

    Optional<StockMovement> findTopByProductIdOrderByIdDesc(Long productId);
}
