package com.ilyas.stockapi.controller;

import com.ilyas.stockapi.exception.BadRequestException;
import com.ilyas.stockapi.exception.ConflictException;
import com.ilyas.stockapi.exception.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.core.PropertyReferenceException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.Map;
import java.util.TreeMap;

/**
 * Gives every error the same JSON shape:
 * {"error": "message"} for most errors, and {"errors": {"field": "message"}} for invalid request bodies.
 * Extending ResponseEntityExceptionHandler also covers the errors Spring MVC raises itself
 * (malformed JSON, wrong HTTP method, unknown URL, ...), which would otherwise use Spring's default format.
 */
@RestControllerAdvice
public class ApiErrorHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiErrorHandler.class);

    // 400 with one message per invalid field, e.g. {"errors": {"price": "must be greater than or equal to 0.00"}}
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        Map<String, String> errors = new TreeMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> errors.putIfAbsent(error.getField(), error.getDefaultMessage()));
        return ResponseEntity.badRequest().body(Map.of("errors", errors));
    }

    // Every other error Spring MVC handles itself (malformed JSON, wrong method, unknown URL, ...)
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return ResponseEntity.status(status).headers(headers).body(Map.of("error", messageFor(ex, status)));
    }

    // The services' own exceptions
    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<Object> handleNotFound(NotFoundException ex) {
        return error(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<Object> handleConflict(ConflictException ex) {
        return error(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<Object> handleBadRequest(BadRequestException ex) {
        return error(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    // Failed login (wrong username or password, disabled account): one message for all,
    // so the response doesn't reveal which usernames exist
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Object> handleAuthentication(AuthenticationException ex) {
        return error(HttpStatus.UNAUTHORIZED, "Invalid username or password");
    }

    // Access checks done inside a controller or service (the URL rules in SecurityConfig
    // answer 403 themselves, before the request gets here)
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Object> handleAccessDenied(AccessDeniedException ex) {
        return error(HttpStatus.FORBIDDEN, "Access denied: your role is not allowed to do this");
    }

    // ?sort=unknownField returns 400 instead of a 500
    @ExceptionHandler({PropertyReferenceException.class, InvalidDataAccessApiUsageException.class})
    public ResponseEntity<Object> handleBadSort(RuntimeException ex) {
        Throwable cause = ex;
        while (cause != null && !(cause instanceof PropertyReferenceException)) {
            cause = cause.getCause();
        }
        if (cause == null) {
            return handleUnexpected(ex);
        }
        String property = ((PropertyReferenceException) cause).getPropertyName();
        return error(HttpStatus.BAD_REQUEST, "Cannot sort by unknown field '" + property + "'");
    }

    // Safety net: a database constraint the checks in the services didn't catch returns 409, not a 500
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Object> handleConstraintViolation(DataIntegrityViolationException ex) {
        return error(HttpStatus.CONFLICT, "The request conflicts with existing data");
    }

    // Anything unexpected: log the details for us, never send them to the client
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleUnexpected(Exception ex) {
        log.error("Unexpected error", ex);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error");
    }

    private static String messageFor(Exception ex, HttpStatusCode status) {
        if (ex instanceof ResponseStatusException rse && rse.getReason() != null) {
            return rse.getReason();
        }
        if (ex instanceof HttpMessageNotReadableException) {
            return "Malformed JSON request body";
        }
        if (ex instanceof MethodArgumentTypeMismatchException mismatch) {
            return "Invalid value '" + mismatch.getValue() + "' for parameter '" + mismatch.getName() + "'";
        }
        if (ex instanceof TypeMismatchException mismatch) {
            return "Invalid value '" + mismatch.getValue() + "' for '" + mismatch.getPropertyName() + "'";
        }
        HttpStatus known = HttpStatus.resolve(status.value());
        return known != null ? known.getReasonPhrase() : "Error";
    }

    private static ResponseEntity<Object> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of("error", message));
    }
}
