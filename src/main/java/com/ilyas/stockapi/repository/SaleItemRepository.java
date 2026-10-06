package com.ilyas.stockapi.repository;

import com.ilyas.stockapi.entity.SaleItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;

public interface SaleItemRepository extends JpaRepository<SaleItem, Long> {

    List<SaleItem> findBySaleId(Long saleId);

    Page<SaleItem> findBySaleId(Long saleId, Pageable pageable);

    long countByProductId(Long productId);

    // Every sold line between two instants (from included, to excluded), in one query for the reports
    @Query("select new com.ilyas.stockapi.repository.SaleLine(s.id, s.saleDate, c.name, p.id, p.name, cat.id, cat.name,"
            + " i.quantity, i.unitPrice)"
            + " from SaleItem i join i.sale s join s.customer c join i.product p left join p.category cat"
            + " where s.saleDate >= :from and s.saleDate < :to order by s.saleDate, s.id, i.id")
    List<SaleLine> findLines(Instant from, Instant to);
}