package com.ilyas.stockapi.repository;

import com.ilyas.stockapi.entity.Sale;
import com.ilyas.stockapi.entity.SaleStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.Optional;

public interface SaleRepository extends JpaRepository<Sale, Long> {

    Page<Sale> findByCustomerId(Long customerId, Pageable pageable);

    long countByCustomerId(Long customerId);

    boolean existsByOrderNumber(String orderNumber);

    Optional<Sale> findByOrderNumber(String orderNumber);

    // Online orders of a phone number that are still in these statuses
    long countByDeliveryPhoneAndStatusIn(String phone, Collection<SaleStatus> statuses);
}