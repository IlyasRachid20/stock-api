package com.ilyas.stockapi.controller;

import com.ilyas.stockapi.dto.ProductRequest;
import com.ilyas.stockapi.dto.ProductResponse;
import com.ilyas.stockapi.entity.Product;
import com.ilyas.stockapi.repository.ProductRepository;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductRepository productRepository;

    public ProductController(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    // GET /api/products?search=galaxy&page=0&size=20&sort=price,desc
    @GetMapping
    public PagedModel<ProductResponse> getAll(
            @RequestParam(required = false) String search,
            @ParameterObject @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        var page = (search == null || search.isBlank())
                ? productRepository.findAll(pageable)
                : productRepository.findByNameContainingIgnoreCase(search.trim(), pageable);
        return new PagedModel<>(page.map(ProductResponse::from));
    }

    @GetMapping("/{id}")
    public ProductResponse getById(@PathVariable Long id) {
        return ProductResponse.from(findOrThrow(id));
    }

    @PostMapping
    public ProductResponse create(@Valid @RequestBody ProductRequest request) {
        Product product = new Product();
        product.setName(request.name());
        product.setPrice(request.price());
        product.setQuantity(request.quantity() != null ? request.quantity() : 0);
        return ProductResponse.from(productRepository.save(product));
    }

    // quantity is optional here: leave it out to change only name and price
    @PutMapping("/{id}")
    public ProductResponse update(@PathVariable Long id, @Valid @RequestBody ProductRequest request) {
        Product product = findOrThrow(id);
        product.setName(request.name());
        product.setPrice(request.price());
        if (request.quantity() != null) {
            product.setQuantity(request.quantity());
        }
        return ProductResponse.from(productRepository.save(product));
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        productRepository.delete(findOrThrow(id));
    }

    private Product findOrThrow(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }
}
