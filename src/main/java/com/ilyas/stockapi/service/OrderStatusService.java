package com.ilyas.stockapi.service;

import com.ilyas.stockapi.entity.OrderStatusChange;
import com.ilyas.stockapi.entity.Sale;
import com.ilyas.stockapi.entity.SaleStatus;
import com.ilyas.stockapi.repository.OrderStatusChangeRepository;
import com.ilyas.stockapi.security.CurrentUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** The story of each online order: every status, with who set it (or "online shop") and when. */
@Service
@Transactional(readOnly = true)
public class OrderStatusService {

    private final OrderStatusChangeRepository changeRepository;

    public OrderStatusService(OrderStatusChangeRepository changeRepository) {
        this.changeRepository = changeRepository;
    }

    @Transactional
    public OrderStatusChange record(Sale sale, SaleStatus status, String note) {
        sale.setStatus(status);
        return changeRepository.save(new OrderStatusChange(sale, status, note, CurrentUser.username()));
    }

    // Oldest first
    public List<OrderStatusChange> historyOf(Long saleId) {
        return changeRepository.findBySaleIdOrderByChangedAtAscIdAsc(saleId);
    }
}
