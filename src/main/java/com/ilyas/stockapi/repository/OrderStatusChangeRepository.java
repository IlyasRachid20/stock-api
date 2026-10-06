package com.ilyas.stockapi.repository;

import com.ilyas.stockapi.entity.OrderStatusChange;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderStatusChangeRepository extends JpaRepository<OrderStatusChange, Long> {

    // Oldest first: the order's story
    List<OrderStatusChange> findBySaleIdOrderByChangedAtAscIdAsc(Long saleId);
}
