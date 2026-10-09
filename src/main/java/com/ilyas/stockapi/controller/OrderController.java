package com.ilyas.stockapi.controller;

import com.ilyas.stockapi.dto.OrderResponses.OrderDetail;
import com.ilyas.stockapi.dto.OrderResponses.OrderSummary;
import com.ilyas.stockapi.dto.OrderResponses.StatusRequest;
import com.ilyas.stockapi.entity.SaleStatus;
import com.ilyas.stockapi.service.OrderManagementService;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

// Online orders for the staff: ADMIN and CASHIER (the cashier calls the customers to confirm)
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderManagementService orderService;

    public OrderController(OrderManagementService orderService) {
        this.orderService = orderService;
    }

    // GET /api/orders?status=NEW&search=TS-7K3 (number, name or phone), newest first by default
    @GetMapping
    public PagedModel<OrderSummary> getAll(
            @RequestParam(required = false) SaleStatus status,
            @RequestParam(required = false) String search,
            @ParameterObject @PageableDefault(size = 20, sort = "saleDate", direction = Sort.Direction.DESC) Pageable pageable) {
        return new PagedModel<>(orderService.find(status, search, pageable));
    }

    // {"NEW": 2, "CONFIRMED": 1, "SHIPPED": 3}: what is waiting at each step
    @GetMapping("/counts")
    public Map<SaleStatus, Long> counts() {
        return orderService.openCounts();
    }

    @GetMapping("/{id}")
    public OrderDetail getById(@PathVariable Long id) {
        return orderService.get(id);
    }

    // Moves the order along; 409 if that step isn't possible from its current status
    @PostMapping("/{id}/status")
    public OrderDetail changeStatus(@PathVariable Long id, @Valid @RequestBody StatusRequest request) {
        return orderService.changeStatus(id, request);
    }
}
