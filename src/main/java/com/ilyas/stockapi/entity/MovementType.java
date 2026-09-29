package com.ilyas.stockapi.entity;

/** Why a product's stock changed. */
public enum MovementType {
    /** Stock given when the product was created */
    INITIAL,
    /** Goods received */
    RESTOCK,
    /** Correction after a count, damage, loss... (positive or negative) */
    ADJUSTMENT,
    /** Sold through a sale item */
    SALE,
    /** A sale item or a whole sale was deleted, so its quantity came back */
    SALE_CANCELLED
}
