package com.ilyas.stockapi.exception;

/** The client sent too many requests of a kind in a short time. Returned as HTTP 429 by ApiErrorHandler. */
public class TooManyRequestsException extends RuntimeException {

    public TooManyRequestsException(String message) {
        super(message);
    }
}
