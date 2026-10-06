package com.ilyas.stockapi.service;

import com.ilyas.stockapi.dto.StockMovementResponse;
import com.ilyas.stockapi.entity.MovementType;
import com.ilyas.stockapi.entity.Product;
import com.ilyas.stockapi.entity.StockMovement;
import com.ilyas.stockapi.repository.StockMovementRepository;
import com.ilyas.stockapi.security.CurrentUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Writes and reads the stock history. Every stock change in the app goes through record(). */
@Service
@Transactional(readOnly = true)
public class StockMovementService {

    private final StockMovementRepository movementRepository;

    public StockMovementService(StockMovementRepository movementRepository) {
        this.movementRepository = movementRepository;
    }

    // Call after the product's quantity has been changed, inside the same transaction
    @Transactional
    public void record(Product product, MovementType type, int quantityChange, String reason, Long saleItemId) {
        StockMovement movement = new StockMovement();
        movement.setProduct(product);
        movement.setType(type);
        movement.setQuantityChange(quantityChange);
        movement.setQuantityAfter(product.getQuantity());
        movement.setReason(reason);
        movement.setSaleItemId(saleItemId);
        movement.setCreatedBy(CurrentUser.username());
        movementRepository.save(movement);
    }

    public Page<StockMovementResponse> find(Long productId, MovementType type, Pageable pageable) {
        return movementRepository.findAll(StockMovementRepository.matching(productId, type), pageable)
                .map(StockMovementResponse::from);
    }

    @Transactional
    public void deleteHistoryOf(Long productId) {
        movementRepository.deleteByProductId(productId);
    }
}
