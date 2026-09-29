package com.ilyas.stockapi.exception;

/** The request breaks a business rule, e.g. not enough stock or an email already used. Returned as HTTP 409 by ApiErrorHandler. */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
