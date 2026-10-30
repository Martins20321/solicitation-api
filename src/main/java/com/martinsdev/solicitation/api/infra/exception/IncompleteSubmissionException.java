package com.martinsdev.solicitation.api.infra.exception;

import java.util.List;

public class IncompleteSubmissionException extends RuntimeException {

    private final List<String> errors;

    public IncompleteSubmissionException(List<String> errors) {
        super("Solicitation submission is incomplete");
        this.errors = errors;
    }

    public List<String> getErrors() {
        return errors;
    }
}
