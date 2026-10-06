package com.ilyas.stockapi.repository;

import com.ilyas.stockapi.entity.ProductImageFile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductImageFileRepository extends JpaRepository<ProductImageFile, Long> {
}
