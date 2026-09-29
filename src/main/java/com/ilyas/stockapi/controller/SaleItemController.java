package com.ilyas.stockapi.controller;

import com.ilyas.stockapi.entity.SaleItem;
import com.ilyas.stockapi.repository.SaleItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sale-items")
public class SaleItemController {

    @Autowired
    private SaleItemRepository saleItemRepository;

    @GetMapping
    public List<SaleItem> getAll() {
        return saleItemRepository.findAll();
    }

    @GetMapping("/{id}")
    public SaleItem getById(@PathVariable Long id) {
        return saleItemRepository.findById(id).orElse(null);
    }

    @PostMapping
    public SaleItem create(@RequestBody SaleItem saleItem) {
        return saleItemRepository.save(saleItem);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        saleItemRepository.deleteById(id);
    }
}