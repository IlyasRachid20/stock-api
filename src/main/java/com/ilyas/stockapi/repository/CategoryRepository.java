package com.ilyas.stockapi.repository;

import com.ilyas.stockapi.dto.CategoryResponse;
import com.ilyas.stockapi.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    Optional<Category> findByNameIgnoreCase(String name);

    // Every category with how many products it holds, in one query, alphabetically
    @Query("select new com.ilyas.stockapi.dto.CategoryResponse(c.id, c.name, count(p.id))"
            + " from Category c left join Product p on p.category = c"
            + " group by c.id, c.name order by lower(c.name)")
    List<CategoryResponse> findAllWithProductCount();
}
