package com.ilyas.stockapi.controller;

import com.ilyas.stockapi.entity.SaleItem;
import com.ilyas.stockapi.repository.SaleItemRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/sale-items")
public class SaleItemController {

    private final SaleItemRepository saleItemRepository;

    public SaleItemController(SaleItemRepository saleItemRepository) {
        this.saleItemRepository = saleItemRepository;
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
        return saleItemRepository.save(saleItem);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        if (!saleItemRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        saleItemRepository.deleteById(id);
    }
}