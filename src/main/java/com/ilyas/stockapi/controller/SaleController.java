package com.ilyas.stockapi.controller;

import com.ilyas.stockapi.entity.Sale;
import com.ilyas.stockapi.repository.SaleRepository;
import com.ilyas.stockapi.service.SaleService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/sales")
public class SaleController {

    private final SaleRepository saleRepository;
    private final SaleService saleService;

    public SaleController(SaleRepository saleRepository, SaleService saleService) {
        this.saleRepository = saleRepository;
        this.saleService = saleService;
    }

    @GetMapping
    public List<Sale> getAll() {
        return saleRepository.findAll();
    }

    @GetMapping("/{id}")
    public Sale getById(@PathVariable Long id) {
        return saleRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @PostMapping
    public Sale create(@Valid @RequestBody Sale sale) {
        return saleService.createSale(sale);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        saleService.deleteSale(id);
    }
}