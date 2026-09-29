package com.ilyas.stockapi.controller;

import com.ilyas.stockapi.dto.SaleItemRequest;
import com.ilyas.stockapi.dto.SaleItemResponse;
import com.ilyas.stockapi.service.SaleService;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sale-items")
public class SaleItemController {

    private final SaleService saleService;

    public SaleItemController(SaleService saleService) {
        this.saleService = saleService;
    }

    // GET /api/sale-items?saleId=1&page=0&size=20
    @GetMapping
    public PagedModel<SaleItemResponse> getAll(
            @RequestParam(required = false) Long saleId,
            @ParameterObject @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return new PagedModel<>(saleService.findItems(saleId, pageable));
    }

    @GetMapping("/{id}")
    public SaleItemResponse getById(@PathVariable Long id) {
        return saleService.getItem(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SaleItemResponse create(@Valid @RequestBody SaleItemRequest request) {
        return saleService.addItem(request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        saleService.removeItem(id);
    }
}
