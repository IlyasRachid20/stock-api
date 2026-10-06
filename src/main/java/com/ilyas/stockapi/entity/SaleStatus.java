package com.ilyas.stockapi.entity;

/**
 * Where a sale stands. A counter sale is COMPLETED at once. An online order (cash on delivery)
 * goes NEW (placed) -> CONFIRMED (the shop called the customer) -> SHIPPED -> DELIVERED (cash
 * collected), or ends CANCELLED (before shipping) or RETURNED (refused at the door): those two put
 * the stock back.
 */
public enum SaleStatus {
    COMPLETED,
    NEW,
    CONFIRMED,
    SHIPPED,
    DELIVERED,
    CANCELLED,
    RETURNED;

    // Revenue is money received: counter sales, and online orders once delivered (cash on delivery)
    public static final java.util.List<SaleStatus> PAID = java.util.List.of(COMPLETED, DELIVERED);

    // Online orders still taking stock and waiting for the shop
    public static final java.util.List<SaleStatus> OPEN = java.util.List.of(NEW, CONFIRMED, SHIPPED);

    // What an online order can become from here: cancelled before it leaves, returned once shipped
    public java.util.List<SaleStatus> next() {
        return switch (this) {
            case NEW -> java.util.List.of(CONFIRMED, CANCELLED);
            case CONFIRMED -> java.util.List.of(SHIPPED, CANCELLED);
            case SHIPPED -> java.util.List.of(DELIVERED, RETURNED);
            default -> java.util.List.of();
        };
    }

    // These put the order's products back in stock
    public boolean returnsStock() {
        return this == CANCELLED || this == RETURNED;
    }
}
