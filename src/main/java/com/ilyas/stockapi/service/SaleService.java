package com.ilyas.stockapi.service;

import com.ilyas.stockapi.dto.SaleItemRequest;
import com.ilyas.stockapi.dto.SaleItemResponse;
import com.ilyas.stockapi.dto.SaleRequest;
import com.ilyas.stockapi.dto.SaleResponse;
import com.ilyas.stockapi.entity.Customer;
import com.ilyas.stockapi.entity.MovementType;
import com.ilyas.stockapi.entity.Product;
import com.ilyas.stockapi.entity.Sale;
import com.ilyas.stockapi.entity.SaleItem;
import com.ilyas.stockapi.exception.BadRequestException;
import com.ilyas.stockapi.exception.ConflictException;
import com.ilyas.stockapi.exception.NotFoundException;
import com.ilyas.stockapi.repository.CustomerRepository;
import com.ilyas.stockapi.repository.ProductRepository;
import com.ilyas.stockapi.repository.SaleItemRepository;
import com.ilyas.stockapi.repository.SaleRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates and deletes sales and sale items while keeping product stock in sync:
 * selling an item takes its quantity out of stock, and deleting it puts the quantity back.
 */
@Service
@Transactional(readOnly = true)
public class SaleService {

    private final SaleRepository saleRepository;
    private final SaleItemRepository saleItemRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final StockMovementService stockMovements;

    public SaleService(SaleRepository saleRepository, SaleItemRepository saleItemRepository,
                       CustomerRepository customerRepository, ProductRepository productRepository,
                       StockMovementService stockMovements) {
        this.saleRepository = saleRepository;
        this.saleItemRepository = saleItemRepository;
        this.customerRepository = customerRepository;
        this.productRepository = productRepository;
        this.stockMovements = stockMovements;
    }

    public Page<SaleResponse> findSales(Long customerId, Pageable pageable) {
        var page = (customerId == null)
                ? saleRepository.findAll(pageable)
                : saleRepository.findByCustomerId(customerId, pageable);
        return page.map(SaleResponse::from);
    }

    public SaleResponse getSale(Long id) {
        return SaleResponse.from(saleRepository.findById(id).orElseThrow(NotFoundException::new));
    }

    public Page<SaleItemResponse> findItems(Long saleId, Pageable pageable) {
        var page = (saleId == null)
                ? saleItemRepository.findAll(pageable)
                : saleItemRepository.findBySaleId(saleId, pageable);
        return page.map(SaleItemResponse::from);
    }

    public SaleItemResponse getItem(Long id) {
        return SaleItemResponse.from(saleItemRepository.findById(id).orElseThrow(NotFoundException::new));
    }

    @Transactional
    public SaleResponse createSale(SaleRequest request) {
        Customer customer = customerRepository.findById(request.customerId())
                .orElseThrow(() -> new BadRequestException("Customer " + request.customerId() + " does not exist"));
        Sale sale = new Sale();
        sale.setCustomer(customer);
        return SaleResponse.from(saleRepository.save(sale));
    }

    @Transactional
    public void deleteSale(Long saleId) {
        Sale sale = saleRepository.findById(saleId).orElseThrow(NotFoundException::new);
        for (SaleItem item : saleItemRepository.findBySaleId(saleId)) {
            returnToStock(item);
            saleItemRepository.delete(item);
        }
        saleRepository.delete(sale);
    }

    @Transactional
    public SaleItemResponse addItem(SaleItemRequest request) {
        Sale sale = saleRepository.findById(request.saleId())
                .orElseThrow(() -> new BadRequestException("Sale " + request.saleId() + " does not exist"));

        // Lock the product row so two sales at the same time can't both take the last units
        Product product = productRepository.findByIdForUpdate(request.productId())
                .orElseThrow(() -> new BadRequestException("Product " + request.productId() + " does not exist"));

        if (product.getQuantity() < request.quantity()) {
            throw new ConflictException("Not enough stock for product '" + product.getName() + "': "
                    + product.getQuantity() + " available, " + request.quantity() + " requested");
        }
        product.setQuantity(product.getQuantity() - request.quantity());

        SaleItem item = new SaleItem();
        item.setQuantity(request.quantity());
        item.setUnitPrice(request.unitPrice() != null ? request.unitPrice() : product.getPrice());
        item.setSale(sale);
        item.setProduct(product);
        sale.getItems().add(item);
        saleItemRepository.save(item);
        stockMovements.record(product, MovementType.SALE, -request.quantity(), "Sale " + sale.getId(), item.getId());
        return SaleItemResponse.from(item);
    }

    @Transactional
    public void removeItem(Long itemId) {
        SaleItem item = saleItemRepository.findById(itemId).orElseThrow(NotFoundException::new);
        returnToStock(item);
        item.getSale().getItems().remove(item);
        saleItemRepository.delete(item);
    }

    private void returnToStock(SaleItem item) {
        Product product = productRepository.findByIdForUpdate(item.getProduct().getId()).orElseThrow();
        product.setQuantity(product.getQuantity() + item.getQuantity());
        stockMovements.record(product, MovementType.SALE_CANCELLED, item.getQuantity(),
                "Sale " + item.getSale().getId() + " item deleted", item.getId());
    }
}
