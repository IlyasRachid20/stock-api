package com.ilyas.stockapi.service;

import com.ilyas.stockapi.dto.AdjustmentRequest;
import com.ilyas.stockapi.dto.PriceChangeResponse;
import com.ilyas.stockapi.dto.ProductRequest;
import com.ilyas.stockapi.dto.ProductResponse;
import com.ilyas.stockapi.dto.RestockRequest;
import com.ilyas.stockapi.entity.Category;
import com.ilyas.stockapi.entity.MovementType;
import com.ilyas.stockapi.entity.Product;
import com.ilyas.stockapi.exception.BadRequestException;
import com.ilyas.stockapi.exception.ConflictException;
import com.ilyas.stockapi.exception.NotFoundException;
import com.ilyas.stockapi.repository.CategoryRepository;
import com.ilyas.stockapi.repository.ProductRepository;
import com.ilyas.stockapi.repository.SaleItemRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;
    private final SaleItemRepository saleItemRepository;
    private final CategoryRepository categoryRepository;
    private final StockMovementService stockMovements;
    private final PriceHistoryService priceHistory;
    private final ProductPictureService pictures;

    public ProductService(ProductRepository productRepository, SaleItemRepository saleItemRepository,
                          CategoryRepository categoryRepository, StockMovementService stockMovements,
                          PriceHistoryService priceHistory, ProductPictureService pictures) {
        this.productRepository = productRepository;
        this.saleItemRepository = saleItemRepository;
        this.categoryRepository = categoryRepository;
        this.stockMovements = stockMovements;
        this.priceHistory = priceHistory;
        this.pictures = pictures;
    }

    // search (part of the name) and categoryId are both optional
    public Page<ProductResponse> find(String search, Long categoryId, Pageable pageable) {
        return productRepository.findAll(ProductRepository.matching(search, categoryId), pageable)
                .map(ProductResponse::from);
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
        product.setDescription(request.description());
        product.setPrice(request.price());
        product.setQuantity(request.quantity() != null ? request.quantity() : 0);
        product.setMinQuantity(request.minQuantity() != null ? request.minQuantity() : 0);
        product.setCategory(categoryOf(request.categoryId()));
        product.setPublished(request.published() == null || request.published());
        productRepository.save(product);
        if (product.getQuantity() > 0) {
            stockMovements.record(product, MovementType.INITIAL, product.getQuantity(), null, null);
        }
        return ProductResponse.from(product);
    }

    // quantity is optional: leave it out to change only name, price and category
    @Transactional
    public ProductResponse update(Long id, ProductRequest request) {
        // Locked like a sale does, so a sale running at the same moment can't have its
        // stock change overwritten by this update (Hibernate writes every column back)
        Product product = productRepository.findByIdForUpdate(id).orElseThrow(NotFoundException::new);
        BigDecimal oldPrice = product.getPrice();
        product.setName(request.name());
        product.setDescription(request.description());
        product.setPrice(request.price());
        product.setCategory(categoryOf(request.categoryId()));
        if (request.published() != null) {
            product.setPublished(request.published());
        }
        if (oldPrice.compareTo(request.price()) != 0) {
            priceHistory.record(product, oldPrice, request.price());
        }
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

    // Every price change of the product, newest first
    public List<PriceChangeResponse> priceHistory(Long id) {
        if (!productRepository.existsById(id)) {
            throw new NotFoundException();
        }
        return priceHistory.historyOf(id);
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
        // Never sold, so its history only holds its own restocks, corrections and price changes
        stockMovements.deleteHistoryOf(id);
        priceHistory.deleteHistoryOf(id);
        pictures.deleteAllOf(id);
        productRepository.delete(product);
    }

    private Category categoryOf(Long categoryId) {
        if (categoryId == null) {
            return null;
        }
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new BadRequestException("Category " + categoryId + " does not exist"));
    }
}
