package com.ilyas.stockapi.service;

import com.ilyas.stockapi.dto.AdjustmentRequest;
import com.ilyas.stockapi.dto.ProductRequest;
import com.ilyas.stockapi.dto.ProductResponse;
import com.ilyas.stockapi.dto.RestockRequest;
import com.ilyas.stockapi.entity.MovementType;
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
    private final StockMovementService stockMovements;

    public ProductService(ProductRepository productRepository, SaleItemRepository saleItemRepository,
                          StockMovementService stockMovements) {
        this.productRepository = productRepository;
        this.saleItemRepository = saleItemRepository;
        this.stockMovements = stockMovements;
    }

    public Page<ProductResponse> find(String search, Pageable pageable) {
        var page = (search == null || search.isBlank())
                ? productRepository.findAll(pageable)
                : productRepository.findByNameContainingIgnoreCase(search.trim(), pageable);
        return page.map(ProductResponse::from);
    }

    // Products at or below their minimum level, emptiest first
    public Page<ProductResponse> findLowStock(Pageable pageable) {
        return productRepository.findLowStock(pageable).map(ProductResponse::from);
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
        product.setMinQuantity(request.minQuantity() != null ? request.minQuantity() : 0);
        productRepository.save(product);
        if (product.getQuantity() > 0) {
            stockMovements.record(product, MovementType.INITIAL, product.getQuantity(), null, null);
        }
        return ProductResponse.from(product);
    }

    // quantity is optional: leave it out to change only name and price
    @Transactional
    public ProductResponse update(Long id, ProductRequest request) {
        // Locked like a sale does, so a sale running at the same moment can't have its
        // stock change overwritten by this update (Hibernate writes every column back)
        Product product = productRepository.findByIdForUpdate(id).orElseThrow(NotFoundException::new);
        product.setName(request.name());
        product.setPrice(request.price());
        if (request.minQuantity() != null) {
            product.setMinQuantity(request.minQuantity());
        }
        if (request.quantity() != null && !request.quantity().equals(product.getQuantity())) {
            int change = request.quantity() - product.getQuantity();
            product.setQuantity(request.quantity());
            stockMovements.record(product, MovementType.ADJUSTMENT, change, "Quantity set by product update", null);
        }
        return ProductResponse.from(product);
    }

    // Goods received
    @Transactional
    public ProductResponse restock(Long id, RestockRequest request) {
        Product product = productRepository.findByIdForUpdate(id).orElseThrow(NotFoundException::new);
        product.setQuantity(product.getQuantity() + request.quantity());
        stockMovements.record(product, MovementType.RESTOCK, request.quantity(), request.reason(), null);
        return ProductResponse.from(product);
    }

    // Correction after a count, damage or loss; stock can't go below 0
    @Transactional
    public ProductResponse adjust(Long id, AdjustmentRequest request) {
        Product product = productRepository.findByIdForUpdate(id).orElseThrow(NotFoundException::new);
        int newQuantity = product.getQuantity() + request.quantityChange();
        if (newQuantity < 0) {
            throw new ConflictException("Cannot remove " + (-request.quantityChange()) + " from product '"
                    + product.getName() + "': only " + product.getQuantity() + " in stock");
        }
        product.setQuantity(newQuantity);
        stockMovements.record(product, MovementType.ADJUSTMENT, request.quantityChange(), request.reason(), null);
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
        // Never sold, so its history only holds its own restocks and corrections
        stockMovements.deleteHistoryOf(id);
        productRepository.delete(product);
    }
}
