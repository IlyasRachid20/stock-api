package com.ilyas.stockapi.controller;

import com.ilyas.stockapi.dto.SaleItemRequest;
import com.ilyas.stockapi.dto.SaleItemResponse;
import com.ilyas.stockapi.repository.SaleItemRepository;
import com.ilyas.stockapi.service.SaleService;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/sale-items")
public class SaleItemController {

    private final SaleItemRepository saleItemRepository;
    private final SaleService saleService;

    public SaleItemController(SaleItemRepository saleItemRepository, SaleService saleService) {
        this.saleItemRepository = saleItemRepository;
        this.saleService = saleService;
    }

    // GET /api/sale-items?saleId=1&page=0&size=20
    @GetMapping
    public PagedModel<SaleItemResponse> getAll(
            @RequestParam(required = false) Long saleId,
            @ParameterObject @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        var page = (saleId == null)
                ? saleItemRepository.findAll(pageable)
                : saleItemRepository.findBySaleId(saleId, pageable);
        return new PagedModel<>(page.map(SaleItemResponse::from));
    }

    @GetMapping("/{id}")
    public SaleItemResponse getById(@PathVariable Long id) {
        return saleItemRepository.findById(id)
                .map(SaleItemResponse::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SaleItemResponse create(@Valid @RequestBody SaleItemRequest request) {
        return SaleItemResponse.from(saleService.addItem(request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        saleService.removeItem(id);
    }
}
