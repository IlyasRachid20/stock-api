package com.ilyas.stockapi.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Body of POST and PUT /api/categories. */
public record CategoryRequest(@NotBlank @Size(max = 50) String name) {

    public CategoryRequest {
        name = (name == null) ? null : name.trim();
    }
}
