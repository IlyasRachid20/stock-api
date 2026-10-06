package com.ilyas.stockapi.dto;

import com.ilyas.stockapi.entity.Product;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * previousPrice is the struck-through price during the 30 days after a price drop, null otherwise.
 * images are the product's pictures, cover first; published is whether the online shop shows it.
 */
public record ProductResponse(Long id, String name, String description, CategorySummary category, BigDecimal price,
                              BigDecimal previousPrice, Integer quantity, Integer minQuantity, boolean lowStock,
                              boolean published, List<ProductImageResponse> images) {

    public static ProductResponse from(Product product) {
        return new ProductResponse(product.getId(), product.getName(), product.getDescription(),
                CategorySummary.from(product.getCategory()), product.getPrice(), product.getPreviousPriceAt(Instant.now()),
                product.getQuantity(), product.getMinQuantity(), product.isLowStock(), product.isPublished(),
                product.getImages().stream().map(ProductImageResponse::from).toList());
    }
}
