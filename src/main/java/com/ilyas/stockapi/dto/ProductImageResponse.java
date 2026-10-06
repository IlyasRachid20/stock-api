package com.ilyas.stockapi.dto;

import com.ilyas.stockapi.entity.ProductImage;

/** A product picture; url serves the JPEG to anyone (pictures are public, like on a shop's website). */
public record ProductImageResponse(Long id, String url, Integer width, Integer height) {

    public static ProductImageResponse from(ProductImage image) {
        return new ProductImageResponse(image.getId(), "/api/images/" + image.getId(), image.getWidth(), image.getHeight());
    }
}
