package com.ilyas.stockapi.service;

import com.ilyas.stockapi.dto.ProductRequest;
import com.ilyas.stockapi.dto.ProductResponse;
import com.ilyas.stockapi.entity.Product;
import com.ilyas.stockapi.exception.ConflictException;
import com.ilyas.stockapi.exception.NotFoundException;
import com.ilyas.stockapi.repository.ProductRepository;
import com.ilyas.stockapi.repository.SaleItemRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;
    private final SaleItemRepository saleItemRepository;

    public ProductService(ProductRepository productRepository, SaleItemRepository saleItemRepository) {
        this.productRepository = productRepository;
        this.saleItemRepository = saleItemRepository;
    }

    public Page<ProductResponse> find(String search, Pageable pageable) {
        var page = (search == null || search.isBlank())
                ? productRepository.findAll(pageable)
                : productRepository.findByNameContainingIgnoreCase(search.trim(), pageable);
        return page.map(ProductResponse::from);
    }

    public ProductResponse get(Long id) {
        return ProductResponse.from(productRepository.findById(id).orElseThrow(NotFoundException::new));
    }

    @Transactional
    public ProductResponse create(ProductRequest request) {
        Product product = new Product();
        product.setName(request.name());
        product.setPrice(request.price());
        product.setQuantity(request.quantity() != null ? request.quantity() : 0);
        return ProductResponse.from(productRepository.save(product));
    }

    // quantity is optional: leave it out to change only name and price
    @Transactional
    public ProductResponse update(Long id, ProductRequest request) {
        // Locked like a sale does, so a sale running at the same moment can't have its
        // stock change overwritten by this update (Hibernate writes every column back)
        Product product = productRepository.findByIdForUpdate(id).orElseThrow(NotFoundException::new);
        product.setName(request.name());
        product.setPrice(request.price());
        if (request.quantity() != null) {
            product.setQuantity(request.quantity());
        }
        return ProductResponse.from(product);
    }

    @Transactional
    public void delete(Long id) {
        Product product = productRepository.findById(id).orElseThrow(NotFoundException::new);
        long sold = saleItemRepository.countByProductId(id);
        if (sold > 0) {
            throw new ConflictException("Product '" + product.getName()
                    + "' cannot be deleted: it appears in " + sold + " sale item(s)");
        }
        productRepository.delete(product);
    }
}
