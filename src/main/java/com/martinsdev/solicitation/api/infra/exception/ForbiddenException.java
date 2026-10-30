package com.martinsdev.solicitation.api.infra.exception;

public class ForbiddenException extends RuntimeException {
    public ForbiddenException() {
        super("Access denied: you are not the owner of this resource");
    }
}
