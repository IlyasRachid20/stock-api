package com.ilyas.stockapi.service;

import com.ilyas.stockapi.dto.CategoryRequest;
import com.ilyas.stockapi.dto.CategoryResponse;
import com.ilyas.stockapi.entity.Category;
import com.ilyas.stockapi.exception.ConflictException;
import com.ilyas.stockapi.exception.NotFoundException;
import com.ilyas.stockapi.repository.CategoryRepository;
import com.ilyas.stockapi.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Product categories. Names are unique, ignoring case: "phones" and "Phones" are the same category. */
@Service
@Transactional(readOnly = true)
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    public CategoryService(CategoryRepository categoryRepository, ProductRepository productRepository) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
    }

    public List<CategoryResponse> findAll() {
        return categoryRepository.findAllWithProductCount();
    }

    @Transactional
    public CategoryResponse create(CategoryRequest request) {
        if (categoryRepository.existsByNameIgnoreCase(request.name())) {
            throw alreadyExists(request.name());
        }
        Category category = new Category();
        category.setName(request.name());
        categoryRepository.save(category);
        return new CategoryResponse(category.getId(), category.getName(), 0);
    }

    @Transactional
    public CategoryResponse update(Long id, CategoryRequest request) {
        Category category = categoryRepository.findById(id).orElseThrow(NotFoundException::new);
        if (categoryRepository.existsByNameIgnoreCaseAndIdNot(request.name(), id)) {
            throw alreadyExists(request.name());
        }
        category.setName(request.name());
        return new CategoryResponse(category.getId(), category.getName(), productRepository.countByCategoryId(id));
    }

    // Only an empty category can be deleted: products are never left pointing to nothing
    @Transactional
    public void delete(Long id) {
        Category category = categoryRepository.findById(id).orElseThrow(NotFoundException::new);
        long products = productRepository.countByCategoryId(id);
        if (products > 0) {
            throw new ConflictException("Category '" + category.getName()
                    + "' cannot be deleted: it has " + products + " product(s)");
        }
        categoryRepository.delete(category);
    }

    private static ConflictException alreadyExists(String name) {
        return new ConflictException("A category named '" + name + "' already exists");
    }
}
