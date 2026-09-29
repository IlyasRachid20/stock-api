package com.ilyas.stockapi.repository;

import com.ilyas.stockapi.entity.SaleItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SaleItemRepository extends JpaRepository<SaleItem, Long> {
}