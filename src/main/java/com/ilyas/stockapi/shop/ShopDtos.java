package com.ilyas.stockapi.shop;

import com.ilyas.stockapi.dto.ProductImageResponse;
import com.ilyas.stockapi.entity.Category;
import com.ilyas.stockapi.entity.Product;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * What the online shop shows, for anyone. Unlike the staff API, the exact stock, the minimum level
 * and the price history are not exposed: only whether a product is available, and "only N left"
 * when few are.
 */
public final class ShopDtos {

    // Below this quantity the shop says "Only 3 left"
    static final int FEW_LEFT = 5;
    // A visitor can put at most this many of one product in the cart (or what's in stock)
    static final int MAX_PER_ORDER = 10;

    private ShopDtos() {
    }

    public enum Availability { IN_STOCK, FEW_LEFT, OUT_OF_STOCK }

    public record CategoryLink(Long id, String slug, String name) {

        static CategoryLink from(Category category) {
            return category == null ? null : new CategoryLink(category.getId(), Slugs.of(category.getName()), category.getName());
        }
    }

    /** A category with its number of products in the shop and a picture for its tile. */
    public record ShopCategory(Long id, String slug, String name, long productCount, String imageUrl) {
    }

    /** A product card: list pages, home page, cart. */
    public record ShopProduct(Long id, String slug, String name, CategoryLink category, BigDecimal price,
                              BigDecimal previousPrice, Availability availability, Integer onlyLeft,
                              int maxQuantity, String imageUrl) {

        static ShopProduct from(Product product, Instant now) {
            int quantity = product.getQuantity();
            return new ShopProduct(product.getId(), Slugs.of(product.getName()), product.getName(),
                    CategoryLink.from(product.getCategory()), product.getPrice(), product.getPreviousPriceAt(now),
                    availabilityOf(quantity), quantity > 0 && quantity <= FEW_LEFT ? quantity : null,
                    Math.min(quantity, MAX_PER_ORDER),
                    product.getImages().isEmpty() ? null : ProductImageResponse.from(product.getImages().get(0)).url());
        }
    }

    /** The product page: the card's details plus the description and every picture. */
    public record ShopProductDetail(Long id, String slug, String name, String description, CategoryLink category,
                                    BigDecimal price, BigDecimal previousPrice, Availability availability,
                                    Integer onlyLeft, int maxQuantity, List<ProductImageResponse> images) {

        static ShopProductDetail from(Product product, Instant now) {
            ShopProduct card = ShopProduct.from(product, now);
            return new ShopProductDetail(card.id(), card.slug(), card.name(), product.getDescription(), card.category(),
                    card.price(), card.previousPrice(), card.availability(), card.onlyLeft(), card.maxQuantity(),
                    product.getImages().stream().map(ProductImageResponse::from).toList());
        }
    }

    /** Everything the home page needs, in one request. */
    public record ShopHome(List<ShopCategory> categories, List<ShopProduct> deals, List<ShopProduct> bestSellers) {
    }

    static Availability availabilityOf(int quantity) {
        if (quantity == 0) {
            return Availability.OUT_OF_STOCK;
        }
        return quantity <= FEW_LEFT ? Availability.FEW_LEFT : Availability.IN_STOCK;
    }
}
