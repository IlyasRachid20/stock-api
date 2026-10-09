package com.ilyas.stockapi.service;

import com.ilyas.stockapi.dto.OrderResponses.OrderDetail;
import com.ilyas.stockapi.dto.OrderResponses.OrderSummary;
import com.ilyas.stockapi.dto.OrderResponses.StatusRequest;
import com.ilyas.stockapi.entity.Sale;
import com.ilyas.stockapi.entity.SaleChannel;
import com.ilyas.stockapi.entity.SaleStatus;
import com.ilyas.stockapi.exception.ConflictException;
import com.ilyas.stockapi.exception.NotFoundException;
import com.ilyas.stockapi.repository.SaleRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Online orders on the staff side: the list, one order, and moving it along
 * NEW -> CONFIRMED -> SHIPPED -> DELIVERED. Cancelling (before shipping) or a return (refused at
 * the door) puts the products back in stock; the order and its lines stay, with who did what.
 */
@Service
@Transactional(readOnly = true)
public class OrderManagementService {

    private final SaleRepository saleRepository;
    private final SaleService saleService;
    private final OrderStatusService statuses;

    public OrderManagementService(SaleRepository saleRepository, SaleService saleService, OrderStatusService statuses) {
        this.saleRepository = saleRepository;
        this.saleService = saleService;
        this.statuses = statuses;
    }

    // status and search (order number, name or phone digits) are optional
    public Page<OrderSummary> find(SaleStatus status, String search, Pageable pageable) {
        Specification<Sale> filter = (root, query, cb) -> {
            List<Predicate> filters = new ArrayList<>();
            filters.add(cb.equal(root.get("channel"), SaleChannel.ONLINE));
            if (status != null) {
                filters.add(cb.equal(root.get("status"), status));
            }
            if (search != null && !search.isBlank()) {
                String text = "%" + search.trim().toLowerCase(Locale.ROOT) + "%";
                String digits = search.replaceAll("\\D", "");
                List<Predicate> anyOf = new ArrayList<>();
                anyOf.add(cb.like(cb.lower(root.get("orderNumber")), text));
                anyOf.add(cb.like(cb.lower(root.get("delivery").get("name")), text));
                if (digits.length() >= 4) {
                    // "0612 34" finds +212612345678: the stored number without its 0 or country code
                    anyOf.add(cb.like(root.get("delivery").get("phone"), "%" + digits.replaceFirst("^0", "") + "%"));
                }
                filters.add(cb.or(anyOf.toArray(Predicate[]::new)));
            }
            return cb.and(filters.toArray(Predicate[]::new));
        };
        return saleRepository.findAll(filter, pageable).map(OrderSummary::from);
    }

    public OrderDetail get(Long id) {
        return detail(onlineOrder(saleRepository.findById(id).orElse(null)));
    }

    // How many orders wait at each step, for the back-office menu
    public Map<SaleStatus, Long> openCounts() {
        Map<SaleStatus, Long> counts = new LinkedHashMap<>();
        SaleStatus.OPEN.forEach(status -> counts.put(status, saleRepository.countByChannelAndStatus(SaleChannel.ONLINE, status)));
        return counts;
    }

    @Transactional
    public OrderDetail changeStatus(Long id, StatusRequest request) {
        Sale sale = onlineOrder(saleRepository.findByIdForUpdate(id).orElse(null));
        SaleStatus target = request.status();
        if (!sale.getStatus().next().contains(target)) {
            throw new ConflictException("Order " + sale.getOrderNumber() + " is " + label(sale.getStatus())
                    + " and can't become " + label(target));
        }
        if (target.returnsStock()) {
            saleService.putBackInStock(sale, "Order " + sale.getOrderNumber() + " " + label(target));
        }
        statuses.record(sale, target, request.note());
        return detail(sale);
    }

    // Orders nobody confirmed in time hold stock for nothing: they are cancelled (see UnconfirmedOrderCanceller)
    @Transactional
    public int cancelUnconfirmed(Instant placedBefore, String note) {
        List<Sale> stale = saleRepository.findByChannelAndStatusAndSaleDateBefore(SaleChannel.ONLINE, SaleStatus.NEW, placedBefore);
        stale.forEach(sale -> changeStatus(sale.getId(), new StatusRequest(SaleStatus.CANCELLED, note)));
        return stale.size();
    }

    private OrderDetail detail(Sale sale) {
        return OrderDetail.from(sale, statuses.historyOf(sale.getId()));
    }

    private static Sale onlineOrder(Sale sale) {
        if (sale == null || sale.getChannel() != SaleChannel.ONLINE) {
            throw new NotFoundException();
        }
        return sale;
    }

    private static String label(SaleStatus status) {
        return status.name().toLowerCase(Locale.ROOT);
    }
}
