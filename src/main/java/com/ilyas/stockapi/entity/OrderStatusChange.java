package com.ilyas.stockapi.entity;

import jakarta.persistence.*;
import java.time.Instant;

/** One status an online order went through. Rows are only ever added, never updated. */
@Entity
@Table(name = "order_status_changes")
public class OrderStatusChange {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sale_id", nullable = false)
    private Sale sale;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SaleStatus status;

    @Column(length = 255)
    private String note;

    @Column(name = "changed_by", nullable = false, length = 50)
    private String changedBy;

    @Column(name = "changed_at", nullable = false)
    private Instant changedAt = Instant.now();

    protected OrderStatusChange() {
    }

    public OrderStatusChange(Sale sale, SaleStatus status, String note, String changedBy) {
        this.sale = sale;
        this.status = status;
        this.note = note;
        this.changedBy = changedBy;
    }

    public Long getId() { return id; }
    public Sale getSale() { return sale; }
    public SaleStatus getStatus() { return status; }
    public String getNote() { return note; }
    public String getChangedBy() { return changedBy; }
    public Instant getChangedAt() { return changedAt; }
    public void setChangedAt(Instant changedAt) { this.changedAt = changedAt; }
}
