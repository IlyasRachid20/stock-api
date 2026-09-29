package com.ilyas.stockapi.controller;

import com.ilyas.stockapi.dto.SaleItemRequest;
import com.ilyas.stockapi.dto.SaleItemResponse;
import com.ilyas.stockapi.repository.SaleItemRepository;
import com.ilyas.stockapi.service.SaleService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/sale-items")
public class SaleItemController {

    private final SaleItemRepository saleItemRepository;
    private final SaleService saleService;

    public SaleItemController(SaleItemRepository saleItemRepository, SaleService saleService) {
        this.saleItemRepository = saleItemRepository;
        this.saleService = saleService;
    }

    @GetMapping
    public List<SaleItemResponse> getAll() {
        return saleItemRepository.findAll().stream().map(SaleItemResponse::from).toList();
    }

    @GetMapping("/{id}")
    public SaleItemResponse getById(@PathVariable Long id) {
        return saleItemRepository.findById(id)
                .map(SaleItemResponse::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @PostMapping
    public SaleItemResponse create(@Valid @RequestBody SaleItemRequest request) {
        return SaleItemResponse.from(saleService.addItem(request));
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        saleService.removeItem(id);
    }
}
