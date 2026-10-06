package com.ilyas.stockapi.repository;

import com.ilyas.stockapi.entity.Product;
import jakarta.persistence.LockModeType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    // Products whose name contains the search text and that are in the category; both filters are
    // optional. Sorting by category.name keeps the products without a category (see SearchText).
    static Specification<Product> matching(String search, Long categoryId) {
        return (root, query, cb) -> {
            List<Predicate> filters = new ArrayList<>();
            if (!SearchText.isBlank(search)) {
                filters.add(SearchText.contains(cb, root.get("name"), search));
            }
            if (categoryId != null) {
                filters.add(cb.equal(root.get("category").get("id"), categoryId));
            }
            return cb.and(filters.toArray(Predicate[]::new));
        };
    }

    // Shown in the online shop
    static Specification<Product> published() {
        return (root, query, cb) -> cb.isTrue(root.get("published"));
    }

    static Specification<Product> withIds(Collection<Long> ids) {
        return (root, query, cb) -> root.get("id").in(ids);
    }

    long countByCategoryId(Long categoryId);

    // The shop's deals: published products whose price dropped since then, latest drop first
    @Query("select p from Product p where p.published = true and p.previousPrice is not null and p.priceReducedAt > :since"
            + " order by p.priceReducedAt desc, p.id")
    List<Product> findPublishedDealsSince(Instant since, Pageable limit);

    @Query("select p from Product p where p.quantity <= p.minQuantity order by p.quantity asc, p.name asc")
    Page<Product> findLowStock(Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Product p where p.id = :id")
    Optional<Product> findByIdForUpdate(Long id);
}