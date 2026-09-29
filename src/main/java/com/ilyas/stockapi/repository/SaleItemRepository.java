package com.ilyas.stockapi.repository;

import com.ilyas.stockapi.entity.SaleItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SaleItemRepository extends JpaRepository<SaleItem, Long> {

    List<SaleItem> findBySaleId(Long saleId);

    Page<SaleItem> findBySaleId(Long saleId, Pageable pageable);
}