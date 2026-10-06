package com.ilyas.stockapi.repository;

import com.ilyas.stockapi.entity.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {

    // Cover first
    List<ProductImage> findByProductIdOrderBySortOrderAscIdAsc(Long productId);

    long countByProductId(Long productId);
}
