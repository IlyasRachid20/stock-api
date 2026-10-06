package com.ilyas.stockapi.controller;

import com.ilyas.stockapi.dto.AdjustmentRequest;
import com.ilyas.stockapi.dto.ProductRequest;
import com.ilyas.stockapi.dto.ProductResponse;
import com.ilyas.stockapi.dto.RestockRequest;
import com.ilyas.stockapi.service.ProductService;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    // GET /api/products?search=galaxy&categoryId=1&page=0&size=20&sort=price,desc
    @GetMapping
    public PagedModel<ProductResponse> getAll(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long categoryId,
            @ParameterObject @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return new PagedModel<>(productService.find(search, categoryId, pageable));
    }

    // What needs reordering: quantity at or below minQuantity, emptiest first
    @GetMapping("/low-stock")
    public PagedModel<ProductResponse> getLowStock(@ParameterObject @PageableDefault(size = 20) Pageable pageable) {
        return new PagedModel<>(productService.findLowStock(pageable));
    }

    @GetMapping("/{id}")
    public ProductResponse getById(@PathVariable Long id) {
        return productService.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductResponse create(@Valid @RequestBody ProductRequest request) {
        return productService.create(request);
    }

    // quantity is optional here: leave it out to change only name, price and category
    @PutMapping("/{id}")
    public ProductResponse update(@PathVariable Long id, @Valid @RequestBody ProductRequest request) {
        return productService.update(id, request);
    }

    // Goods received: adds to the stock and records a RESTOCK movement (ADMIN only)
    @PostMapping("/{id}/restock")
    public ProductResponse restock(@PathVariable Long id, @Valid @RequestBody RestockRequest request) {
        return productService.restock(id, request);
    }

    // Correction after a count, damage or loss, with a required reason (ADMIN only)
    @PostMapping("/{id}/adjustments")
    public ProductResponse adjust(@PathVariable Long id, @Valid @RequestBody AdjustmentRequest request) {
        return productService.adjust(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        productService.delete(id);
    }
}
