package com.ilyas.stockapi.controller;

import com.ilyas.stockapi.dto.SaleRequest;
import com.ilyas.stockapi.dto.SaleResponse;
import com.ilyas.stockapi.repository.SaleRepository;
import com.ilyas.stockapi.service.SaleService;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/sales")
public class SaleController {

    private final SaleRepository saleRepository;
    private final SaleService saleService;

    public SaleController(SaleRepository saleRepository, SaleService saleService) {
        this.saleRepository = saleRepository;
        this.saleService = saleService;
    }

    // GET /api/sales?customerId=1&page=0&size=20 (newest first by default)
    @GetMapping
    public PagedModel<SaleResponse> getAll(
            @RequestParam(required = false) Long customerId,
            @ParameterObject @PageableDefault(size = 20, sort = {"saleDate", "id"}, direction = Sort.Direction.DESC) Pageable pageable) {
        var page = (customerId == null)
                ? saleRepository.findAll(pageable)
                : saleRepository.findByCustomerId(customerId, pageable);
        return new PagedModel<>(page.map(SaleResponse::from));
    }

    @GetMapping("/{id}")
    public SaleResponse getById(@PathVariable Long id) {
        return saleRepository.findById(id)
                .map(SaleResponse::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @PostMapping
    public SaleResponse create(@Valid @RequestBody SaleRequest request) {
        return SaleResponse.from(saleService.createSale(request));
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        saleService.deleteSale(id);
    }
}
