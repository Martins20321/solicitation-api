package com.martinsdev.solicitation.api.infra.handler;

import com.martinsdev.solicitation.api.infra.exception.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleEmailAlreadyExists(EmailAlreadyExistsException ex, HttpServletRequest request) {
        String error = "Email Already Exists";
        HttpStatus status = HttpStatus.CONFLICT;
        ErrorResponse errorResponse = new ErrorResponse(Instant.now(), status.value(), error, ex.getMessage(), request.getRequestURI());
        return ResponseEntity.status(status).body(errorResponse);
    }

    @ExceptionHandler(InvalidRoleException.class)
    public ResponseEntity<ErrorResponse> handleInvalidRole(InvalidRoleException ex, HttpServletRequest request) {
        String error = "Invalid Role";
        HttpStatus status = HttpStatus.BAD_REQUEST;
        ErrorResponse errorResponse = new ErrorResponse(Instant.now(), status.value(), error, ex.getMessage(), request.getRequestURI());
        return ResponseEntity.status(status).body(errorResponse);
    }

    @ExceptionHandler(InvalidOperationException.class)
    public ResponseEntity<ErrorResponse> handleInvalidOperation(InvalidOperationException ex, HttpServletRequest request) {
        String error = "Invalid Operation";
        HttpStatus status = HttpStatus.BAD_REQUEST;
        ErrorResponse errorResponse = new ErrorResponse(Instant.now(), status.value(), error, ex.getMessage(), request.getRequestURI());
        return ResponseEntity.status(status).body(errorResponse);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        String error = "Resource Not Found";
        HttpStatus status = HttpStatus.NOT_FOUND;
        ErrorResponse errorResponse = new ErrorResponse(Instant.now(), status.value(), error, ex.getMessage(), request.getRequestURI());
        return ResponseEntity.status(status).body(errorResponse);
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ErrorResponse> handleForbidden(ForbiddenException ex, HttpServletRequest request) {
        String error = "Access Denied";
        HttpStatus status = HttpStatus.FORBIDDEN;
        ErrorResponse errorResponse = new ErrorResponse(Instant.now(), status.value(), error, ex.getMessage(), request.getRequestURI());
        return ResponseEntity.status(status).body(errorResponse);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String error = "Validation Error";
        String message = ex.getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage()).collect(Collectors.joining(", "));
        HttpStatus status = HttpStatus.BAD_REQUEST;
        ErrorResponse errorResponse = new ErrorResponse(Instant.now(), status.value(), error, message, request.getRequestURI());
        return ResponseEntity.status(status).body(errorResponse);
    }

    @ExceptionHandler(IncompleteSubmissionException.class)
    public ResponseEntity<ErrorResponse> handleIncompleteSubmissionNotValid(IncompleteSubmissionException ex, HttpServletRequest request) {
        String error = ex.getMessage();
        String message = String.join(", ", ex.getErrors());
        HttpStatus status = HttpStatus.BAD_REQUEST;
        ErrorResponse errorResponse = new ErrorResponse(Instant.now(), status.value(), error, message, request.getRequestURI());
        return ResponseEntity.status(status).body(errorResponse);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(DataIntegrityViolationException ex, HttpServletRequest request) {
        String error = "Data Integrity Violation";
        String message = "A resource with the given data already exists";
        HttpStatus status = HttpStatus.CONFLICT;
        ErrorResponse errorResponse = new ErrorResponse(Instant.now(), status.value(), error, message, request.getRequestURI());
        return ResponseEntity.status(status).body(errorResponse);
    }
}
