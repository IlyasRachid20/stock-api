package com.ilyas.stockapi.entity;

import jakarta.persistence.*;
import java.time.Instant;

/** One change to a product's stock. Rows are only ever added, never updated. */
@Entity
@Table(name = "stock_movements")
public class StockMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MovementType type;

    // +10 for a restock of 10, -2 for a sale of 2
    @Column(name = "quantity_change", nullable = false)
    private Integer quantityChange;

    // The product's stock right after this change
    @Column(name = "quantity_after", nullable = false)
    private Integer quantityAfter;

    @Column(length = 255)
    private String reason;

    // Plain id, not a relation: the sale item may be deleted later, the history must stay
    @Column(name = "sale_item_id")
    private Long saleItemId;

    @Column(name = "created_by", nullable = false, length = 50)
    private String createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    // Getters and setters
    public Long getId() { return id; }

    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }

    public MovementType getType() { return type; }
    public void setType(MovementType type) { this.type = type; }

    public Integer getQuantityChange() { return quantityChange; }
    public void setQuantityChange(Integer quantityChange) { this.quantityChange = quantityChange; }

    public Integer getQuantityAfter() { return quantityAfter; }
    public void setQuantityAfter(Integer quantityAfter) { this.quantityAfter = quantityAfter; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public Long getSaleItemId() { return saleItemId; }
    public void setSaleItemId(Long saleItemId) { this.saleItemId = saleItemId; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public Instant getCreatedAt() { return createdAt; }
}
