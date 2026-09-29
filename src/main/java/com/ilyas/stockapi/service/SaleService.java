package com.ilyas.stockapi.service;

import com.ilyas.stockapi.dto.SaleItemRequest;
import com.ilyas.stockapi.dto.SaleRequest;
import com.ilyas.stockapi.entity.Customer;
import com.ilyas.stockapi.entity.Product;
import com.ilyas.stockapi.entity.Sale;
import com.ilyas.stockapi.entity.SaleItem;
import com.ilyas.stockapi.repository.CustomerRepository;
import com.ilyas.stockapi.repository.ProductRepository;
import com.ilyas.stockapi.repository.SaleItemRepository;
import com.ilyas.stockapi.repository.SaleRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Creates and deletes sales and sale items while keeping product stock in sync:
 * selling an item takes its quantity out of stock, and deleting it puts the quantity back.
 */
@Service
public class SaleService {

    private final SaleRepository saleRepository;
    private final SaleItemRepository saleItemRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;

    public SaleService(SaleRepository saleRepository, SaleItemRepository saleItemRepository,
                       CustomerRepository customerRepository, ProductRepository productRepository) {
        this.saleRepository = saleRepository;
        this.saleItemRepository = saleItemRepository;
        this.customerRepository = customerRepository;
        this.productRepository = productRepository;
    }

    @Transactional
    public Sale createSale(SaleRequest request) {
        Customer customer = customerRepository.findById(request.customerId())
                .orElseThrow(() -> badRequest("Customer " + request.customerId() + " does not exist"));
        Sale sale = new Sale();
        sale.setCustomer(customer);
        return saleRepository.save(sale);
    }

    @Transactional
    public void deleteSale(Long saleId) {
        Sale sale = saleRepository.findById(saleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        for (SaleItem item : saleItemRepository.findBySaleId(saleId)) {
            returnToStock(item);
            saleItemRepository.delete(item);
        }
        saleRepository.delete(sale);
    }

    @Transactional
    public SaleItem addItem(SaleItemRequest request) {
        Sale sale = saleRepository.findById(request.saleId())
                .orElseThrow(() -> badRequest("Sale " + request.saleId() + " does not exist"));

        // Lock the product row so two sales at the same time can't both take the last units
        Product product = productRepository.findByIdForUpdate(request.productId())
                .orElseThrow(() -> badRequest("Product " + request.productId() + " does not exist"));

        if (product.getQuantity() < request.quantity()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Not enough stock for product '" + product.getName() + "': "
                            + product.getQuantity() + " available, " + request.quantity() + " requested");
        }
        product.setQuantity(product.getQuantity() - request.quantity());

        SaleItem item = new SaleItem();
        item.setQuantity(request.quantity());
        item.setUnitPrice(request.unitPrice() != null ? request.unitPrice() : product.getPrice());
        item.setSale(sale);
        item.setProduct(product);
        sale.getItems().add(item);
        return saleItemRepository.save(item);
    }

    @Transactional
    public void removeItem(Long itemId) {
        SaleItem item = saleItemRepository.findById(itemId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        returnToStock(item);
        item.getSale().getItems().remove(item);
        saleItemRepository.delete(item);
    }

    private void returnToStock(SaleItem item) {
        Product product = productRepository.findByIdForUpdate(item.getProduct().getId()).orElseThrow();
        product.setQuantity(product.getQuantity() + item.getQuantity());
    }

    private static ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
