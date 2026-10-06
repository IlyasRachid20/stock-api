package com.ilyas.stockapi.dto;

import com.ilyas.stockapi.entity.Category;

/** Short category info shown inside a product; null when the product has no category. */
public record CategorySummary(Long id, String name) {

    public static CategorySummary from(Category category) {
        return (category == null) ? null : new CategorySummary(category.getId(), category.getName());
    }
}
