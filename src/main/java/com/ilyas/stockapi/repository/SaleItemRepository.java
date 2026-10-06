package com.ilyas.stockapi.repository;

import com.ilyas.stockapi.entity.SaleItem;
import com.ilyas.stockapi.entity.SaleStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

public interface SaleItemRepository extends JpaRepository<SaleItem, Long> {

    List<SaleItem> findBySaleId(Long saleId);

    Page<SaleItem> findBySaleId(Long saleId, Pageable pageable);

    long countByProductId(Long productId);

    // The products sold the most since then, by quantity
    @Query("select i.product.id from SaleItem i where i.sale.saleDate >= :since and i.sale.status in :statuses"
            + " group by i.product.id order by sum(i.quantity) desc, i.product.id")
    List<Long> findBestSellingProductIds(Instant since, Collection<SaleStatus> statuses, Pageable limit);

    // Every sold line between two instants (from included, to excluded), in one query for the reports
    @Query("select new com.ilyas.stockapi.repository.SaleLine(s.id, s.saleDate, s.channel, c.name, p.id, p.name, cat.id, cat.name,"
            + " i.quantity, i.unitPrice)"
            + " from SaleItem i join i.sale s join s.customer c join i.product p left join p.category cat"
            + " where s.saleDate >= :from and s.saleDate < :to and s.status in :statuses order by s.saleDate, s.id, i.id")
    List<SaleLine> findLines(Instant from, Instant to, Collection<SaleStatus> statuses);
}