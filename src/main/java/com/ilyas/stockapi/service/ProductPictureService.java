package com.ilyas.stockapi.service;

import com.ilyas.stockapi.dto.ProductImageResponse;
import com.ilyas.stockapi.entity.Product;
import com.ilyas.stockapi.entity.ProductImage;
import com.ilyas.stockapi.entity.ProductImageFile;
import com.ilyas.stockapi.exception.BadRequestException;
import com.ilyas.stockapi.exception.ConflictException;
import com.ilyas.stockapi.exception.NotFoundException;
import com.ilyas.stockapi.repository.ProductImageFileRepository;
import com.ilyas.stockapi.repository.ProductImageRepository;
import com.ilyas.stockapi.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Comparator;
import java.util.List;

/**
 * Product pictures, stored in the database (a free host's disk is wiped on every restart).
 * Each upload is re-encoded as a JPEG of at most 1200 pixels (see Pictures), about 100 KB.
 */
@Service
@Transactional(readOnly = true)
public class ProductPictureService {

    public static final int MAX_PICTURES = 6;
    public static final long MAX_UPLOAD_BYTES = 10L * 1024 * 1024;

    private final ProductRepository productRepository;
    private final ProductImageRepository imageRepository;
    private final ProductImageFileRepository fileRepository;

    public ProductPictureService(ProductRepository productRepository, ProductImageRepository imageRepository,
                                 ProductImageFileRepository fileRepository) {
        this.productRepository = productRepository;
        this.imageRepository = imageRepository;
        this.fileRepository = fileRepository;
    }

    // Adds the picture after the existing ones (the first one stays the cover)
    @Transactional
    public ProductImageResponse add(Long productId, InputStream upload, long size) {
        Product product = productRepository.findById(productId).orElseThrow(NotFoundException::new);
        List<ProductImage> pictures = imageRepository.findByProductIdOrderBySortOrderAscIdAsc(productId);
        if (pictures.size() >= MAX_PICTURES) {
            throw new ConflictException("A product can have " + MAX_PICTURES + " pictures at most: delete one first");
        }
        if (size == 0) {
            throw new BadRequestException("The file is empty");
        }
        if (size > MAX_UPLOAD_BYTES) {
            throw new BadRequestException("The file is too large: 10 MB at most");
        }

        Pictures.Jpeg jpeg;
        try {
            jpeg = Pictures.toJpeg(upload);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        ProductImage image = new ProductImage();
        image.setProduct(product);
        image.setSortOrder(pictures.isEmpty() ? 0 : pictures.get(pictures.size() - 1).getSortOrder() + 1);
        image.setWidth(jpeg.width());
        image.setHeight(jpeg.height());
        imageRepository.save(image);
        fileRepository.save(new ProductImageFile(image.getId(), jpeg.content()));
        product.getImages().add(image); // keeps the product's list right for the rest of the transaction
        return ProductImageResponse.from(image);
    }

    // Makes the picture the product's cover (first in the list)
    @Transactional
    public List<ProductImageResponse> makeCover(Long productId, Long imageId) {
        ProductImage cover = pictureOf(productId, imageId);
        List<ProductImage> pictures = cover.getProduct().getImages();
        cover.setSortOrder(pictures.get(0).getSortOrder() - 1);
        pictures.sort(Comparator.comparing(ProductImage::getSortOrder).thenComparing(ProductImage::getId));
        return pictures.stream().map(ProductImageResponse::from).toList();
    }

    @Transactional
    public void delete(Long productId, Long imageId) {
        ProductImage image = pictureOf(productId, imageId);
        image.getProduct().getImages().remove(image);
        fileRepository.deleteById(image.getId());
        imageRepository.delete(image);
    }

    // When a product is deleted
    @Transactional
    public void deleteAllOf(Long productId) {
        for (ProductImage image : imageRepository.findByProductIdOrderBySortOrderAscIdAsc(productId)) {
            fileRepository.deleteById(image.getId());
            imageRepository.delete(image);
        }
    }

    public byte[] content(Long imageId) {
        return fileRepository.findById(imageId).map(ProductImageFile::getContent).orElseThrow(NotFoundException::new);
    }

    private ProductImage pictureOf(Long productId, Long imageId) {
        return imageRepository.findById(imageId)
                .filter(image -> image.getProduct().getId().equals(productId))
                .orElseThrow(NotFoundException::new);
    }
}
