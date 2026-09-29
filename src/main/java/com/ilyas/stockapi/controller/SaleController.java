package com.ilyas.stockapi.controller;

import com.ilyas.stockapi.entity.Sale;
import com.ilyas.stockapi.repository.SaleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/sales")
public class SaleController {

    @Autowired
    private SaleRepository saleRepository;

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
    public Sale create(@RequestBody Sale sale) {
        return saleRepository.save(sale);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        if (!saleRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        saleRepository.deleteById(id);
    }
}