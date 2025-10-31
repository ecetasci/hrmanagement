package com.ecetasci.hrmanagement.exceptions;

public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String message) {
        super(message);
    }

    public ForbiddenException() {
        super("Forbidden");
    }
}

