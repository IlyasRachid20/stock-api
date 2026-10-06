package com.ilyas.stockapi.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;

@Entity
@Table(name = "products")
public class Product {

    // How long the old price stays struck through after a price drop
    public static final Duration REDUCTION_SHOWN_FOR = Duration.ofDays(30);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private Integer quantity = 0;

    // Low-stock alert when quantity is at or below this level
    @Column(name = "min_quantity", nullable = false)
    private Integer minQuantity = 0;

    // Optional: products without a category are "uncategorized"
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    // Set by PriceHistoryService after a price drop: the struck-through price, and when the drop happened
    @Column(name = "previous_price", precision = 10, scale = 2)
    private BigDecimal previousPrice;

    @Column(name = "price_reduced_at")
    private Instant priceReducedAt;

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public Integer getMinQuantity() { return minQuantity; }
    public void setMinQuantity(Integer minQuantity) { this.minQuantity = minQuantity; }

    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }

    public BigDecimal getPreviousPrice() { return previousPrice; }
    public void setPreviousPrice(BigDecimal previousPrice) { this.previousPrice = previousPrice; }

    public Instant getPriceReducedAt() { return priceReducedAt; }
    public void setPriceReducedAt(Instant priceReducedAt) { this.priceReducedAt = priceReducedAt; }

    // The struck-through price to show at that moment: only during the 30 days after a drop
    public BigDecimal getPreviousPriceAt(Instant now) {
        boolean recent = priceReducedAt != null && priceReducedAt.isAfter(now.minus(REDUCTION_SHOWN_FOR));
        return (recent && previousPrice != null && previousPrice.compareTo(price) > 0) ? previousPrice : null;
    }

    public boolean isLowStock() { return quantity <= minQuantity; }
}