package com.ilyas.stockapi.repository;

import com.ilyas.stockapi.entity.Sale;
import com.ilyas.stockapi.entity.SaleStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.ilyas.stockapi.entity.SaleChannel;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface SaleRepository extends JpaRepository<Sale, Long>, JpaSpecificationExecutor<Sale> {

    Page<Sale> findByCustomerId(Long customerId, Pageable pageable);

    long countByCustomerId(Long customerId);

    boolean existsByOrderNumber(String orderNumber);

    Optional<Sale> findByOrderNumber(String orderNumber);

    // Online orders of a phone number that are still in these statuses
    long countByDeliveryPhoneAndStatusIn(String phone, Collection<SaleStatus> statuses);

    long countByChannelAndStatus(SaleChannel channel, SaleStatus status);

    // Locked while its status changes, so two people clicking at once can't both put the stock back
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Sale s where s.id = :id")
    Optional<Sale> findByIdForUpdate(Long id);

    // Online orders still in that status, placed before then (for the 48-hour cancellation)
    List<Sale> findByChannelAndStatusAndSaleDateBefore(SaleChannel channel, SaleStatus status, Instant before);
}