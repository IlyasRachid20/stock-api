package com.ilyas.stockapi.exception;

/** The request refers to something invalid, e.g. a customer id that doesn't exist. Returned as HTTP 400 by ApiErrorHandler. */
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }
}
