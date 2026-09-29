package com.ilyas.stockapi.controller;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.core.PropertyReferenceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.TreeMap;

@RestControllerAdvice
public class ApiErrorHandler {

    // Returns 400 with one message per invalid field, e.g. {"errors": {"price": "must be greater than or equal to 0.00"}}
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Map<String, String>> handleInvalidBody(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new TreeMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> errors.putIfAbsent(error.getField(), error.getDefaultMessage()));
        return Map.of("errors", errors);
    }

    // Returns the status of a ResponseStatusException with its reason, e.g. 409 {"error": "Email ... is already used"}
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> handleStatus(ResponseStatusException ex) {
        String message = ex.getReason() != null ? ex.getReason() : HttpStatus.valueOf(ex.getStatusCode().value()).getReasonPhrase();
        return ResponseEntity.status(ex.getStatusCode()).body(Map.of("error", message));
    }

    // ?sort=unknownField returns 400 instead of a 500
    @ExceptionHandler({PropertyReferenceException.class, InvalidDataAccessApiUsageException.class})
    public ResponseEntity<Map<String, String>> handleBadSort(RuntimeException ex) {
        Throwable cause = ex;
        while (cause != null && !(cause instanceof PropertyReferenceException)) {
            cause = cause.getCause();
        }
        if (cause == null) {
            throw ex;
        }
        String property = ((PropertyReferenceException) cause).getPropertyName();
        return ResponseEntity.badRequest().body(Map.of("error", "Cannot sort by unknown field '" + property + "'"));
    }

    // Safety net: a database constraint the checks above didn't catch returns 409, not a 500
    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Map<String, String> handleConstraintViolation(DataIntegrityViolationException ex) {
        return Map.of("error", "The request conflicts with existing data");
    }
}
