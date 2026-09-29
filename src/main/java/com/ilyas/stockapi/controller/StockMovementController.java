package com.ilyas.stockapi.controller;

import com.ilyas.stockapi.dto.StockMovementResponse;
import com.ilyas.stockapi.entity.MovementType;
import com.ilyas.stockapi.service.StockMovementService;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.web.bind.annotation.*;

// Read-only: movements are only created by the operations that change stock
@RestController
@RequestMapping("/api/stock-movements")
public class StockMovementController {

    private final StockMovementService stockMovementService;

    public StockMovementController(StockMovementService stockMovementService) {
        this.stockMovementService = stockMovementService;
    }

    // GET /api/stock-movements?productId=1&type=SALE (newest first by default)
    @GetMapping
    public PagedModel<StockMovementResponse> getAll(
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) MovementType type,
            @ParameterObject @PageableDefault(size = 20, sort = {"createdAt", "id"}, direction = Sort.Direction.DESC) Pageable pageable) {
        return new PagedModel<>(stockMovementService.find(productId, type, pageable));
    }
}
