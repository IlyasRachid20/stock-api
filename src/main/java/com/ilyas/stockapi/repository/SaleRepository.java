package com.ilyas.stockapi.repository;

import com.ilyas.stockapi.entity.Sale;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SaleRepository extends JpaRepository<Sale, Long> {

    Page<Sale> findByCustomerId(Long customerId, Pageable pageable);

    long countByCustomerId(Long customerId);
}