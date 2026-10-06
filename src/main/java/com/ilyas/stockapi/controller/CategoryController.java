package com.ilyas.stockapi.controller;

import com.ilyas.stockapi.dto.CategoryRequest;
import com.ilyas.stockapi.dto.CategoryResponse;
import com.ilyas.stockapi.service.CategoryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Everyone can read the categories; creating, renaming and deleting is for ADMIN (see SecurityConfig)
@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    // All categories, alphabetically, with their number of products (a shop has a few, so no paging)
    @GetMapping
    public List<CategoryResponse> getAll() {
        return categoryService.findAll();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryResponse create(@Valid @RequestBody CategoryRequest request) {
        return categoryService.create(request);
    }

    @PutMapping("/{id}")
    public CategoryResponse update(@PathVariable Long id, @Valid @RequestBody CategoryRequest request) {
        return categoryService.update(id, request);
    }

    // 409 while products are in it: move or delete them first
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        categoryService.delete(id);
    }
}
