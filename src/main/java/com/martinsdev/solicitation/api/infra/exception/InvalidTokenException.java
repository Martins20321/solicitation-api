package com.martinsdev.solicitation.api.infra.exception;

public class InvalidTokenException extends RuntimeException {
    public InvalidTokenException() {
        super("Invalid or expired JWT token!");
    }
}
