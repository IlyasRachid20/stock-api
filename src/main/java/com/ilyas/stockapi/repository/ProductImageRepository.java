package com.ilyas.stockapi.repository;

import com.ilyas.stockapi.entity.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {

    // Cover first
    List<ProductImage> findByProductIdOrderBySortOrderAscIdAsc(Long productId);

    long countByProductId(Long productId);

    // [category id, picture id]: one picture (the oldest) of each category's published products, for the shop's tiles
    @Query("select p.category.id, min(i.id) from ProductImage i join i.product p"
            + " where p.published = true and p.category is not null group by p.category.id")
    List<Object[]> findOnePicturePerCategory();
}
