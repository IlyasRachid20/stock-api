package com.ilyas.stockapi.exception;

/** A sale asks for more than is in stock. A 409, with the product's name for a friendlier message. */
public class NotEnoughStockException extends ConflictException {

    private final String productName;

    public NotEnoughStockException(String productName, int available, int requested) {
        super("Not enough stock for product '" + productName + "': " + available + " available, " + requested + " requested");
        this.productName = productName;
    }

    public String getProductName() {
        return productName;
    }
}
