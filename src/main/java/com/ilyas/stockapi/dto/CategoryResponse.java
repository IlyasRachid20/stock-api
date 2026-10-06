package com.ilyas.stockapi.dto;

/** A category and the number of products in it. */
public record CategoryResponse(Long id, String name, long productCount) {
}
