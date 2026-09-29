package com.ilyas.stockapi.exception;

/** The requested resource doesn't exist. Returned as HTTP 404 by ApiErrorHandler. */
public class NotFoundException extends RuntimeException {

    public NotFoundException() {
        super("Not Found");
    }

    public NotFoundException(String message) {
        super(message);
    }
}
