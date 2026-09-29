package com.ilyas.stockapi.controller;

import com.ilyas.stockapi.entity.SaleItem;
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
    public List<SaleItem> getAll() {
        return saleItemRepository.findAll();
    }

    @GetMapping("/{id}")
    public SaleItem getById(@PathVariable Long id) {
        return saleItemRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @PostMapping
    public SaleItem create(@Valid @RequestBody SaleItem saleItem) {
        return saleService.addItem(saleItem);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        saleService.removeItem(id);
    }
}