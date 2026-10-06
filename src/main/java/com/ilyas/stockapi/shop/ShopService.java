package com.ilyas.stockapi.shop;

import com.ilyas.stockapi.entity.Category;
import com.ilyas.stockapi.entity.Product;
import com.ilyas.stockapi.entity.SaleStatus;
import com.ilyas.stockapi.exception.NotFoundException;
import com.ilyas.stockapi.repository.CategoryRepository;
import com.ilyas.stockapi.repository.ProductImageRepository;
import com.ilyas.stockapi.repository.ProductRepository;
import com.ilyas.stockapi.repository.SaleItemRepository;
import com.ilyas.stockapi.shop.ShopDtos.ShopCategory;
import com.ilyas.stockapi.shop.ShopDtos.ShopHome;
import com.ilyas.stockapi.shop.ShopDtos.ShopProduct;
import com.ilyas.stockapi.shop.ShopDtos.ShopProductDetail;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** The online shop's catalog: only published products, read-only, open to everyone. */
@Service
@Transactional(readOnly = true)
public class ShopService {

    private static final int HOME_LIST_SIZE = 8;
    private static final Duration BEST_SELLER_PERIOD = Duration.ofDays(30);

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductImageRepository imageRepository;
    private final SaleItemRepository saleItemRepository;

    public ShopService(ProductRepository productRepository, CategoryRepository categoryRepository,
                       ProductImageRepository imageRepository, SaleItemRepository saleItemRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.imageRepository = imageRepository;
        this.saleItemRepository = saleItemRepository;
    }

    // The categories, the current deals (recent price drops) and the best sellers of the last 30 days
    public ShopHome home() {
        Instant now = Instant.now();
        List<ShopProduct> deals = productRepository
                .findPublishedDealsSince(now.minus(Product.REDUCTION_SHOWN_FOR), PageRequest.of(0, HOME_LIST_SIZE)).stream()
                .map(product -> ShopProduct.from(product, now))
                .filter(card -> card.previousPrice() != null)
                .toList();

        // A few extra ids, since some best sellers may be hidden from the shop now
        List<Long> bestIds = saleItemRepository.findBestSellingProductIds(
                now.minus(BEST_SELLER_PERIOD), SaleStatus.PAID, PageRequest.of(0, HOME_LIST_SIZE * 2));
        Map<Long, Product> byId = productRepository.findAllById(bestIds).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));
        List<ShopProduct> bestSellers = bestIds.stream()
                .map(byId::get)
                .filter(product -> product != null && product.isPublished())
                .limit(HOME_LIST_SIZE)
                .map(product -> ShopProduct.from(product, now))
                .toList();

        return new ShopHome(categories(), deals, bestSellers);
    }

    // Categories that have products in the shop, each with one of their pictures for its tile
    public List<ShopCategory> categories() {
        Map<Long, Long> pictureByCategory = imageRepository.findOnePicturePerCategory().stream()
                .collect(Collectors.toMap(row -> (Long) row[0], row -> (Long) row[1]));
        return categoryRepository.findAllWithPublishedProductCount().stream()
                .map(category -> new ShopCategory(category.id(), Slugs.of(category.name()), category.name(),
                        category.productCount(),
                        pictureByCategory.containsKey(category.id()) ? "/api/images/" + pictureByCategory.get(category.id()) : null))
                .toList();
    }

    // Every filter is optional: the category's slug, part of the name, or a list of ids (the cart)
    public Page<ShopProduct> products(String categorySlug, String search, Collection<Long> ids, Pageable pageable) {
        Long categoryId = (categorySlug == null || categorySlug.isBlank()) ? null : categoryBySlug(categorySlug).getId();
        Specification<Product> filter = ProductRepository.matching(search, categoryId).and(ProductRepository.published());
        if (ids != null && !ids.isEmpty()) {
            filter = filter.and(ProductRepository.withIds(ids));
        }
        Instant now = Instant.now();
        return productRepository.findAll(filter, pageable).map(product -> ShopProduct.from(product, now));
    }

    // A hidden product is "not found" for the shop
    public ShopProductDetail product(Long id) {
        Product product = productRepository.findById(id).filter(Product::isPublished).orElseThrow(NotFoundException::new);
        return ShopProductDetail.from(product, Instant.now());
    }

    // Categories are few: matching the slug in Java avoids storing it
    private Category categoryBySlug(String slug) {
        return categoryRepository.findAll().stream()
                .filter(category -> Slugs.of(category.getName()).equals(slug))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("No category '" + slug + "' in the shop"));
    }
}
