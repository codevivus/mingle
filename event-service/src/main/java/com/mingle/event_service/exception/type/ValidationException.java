package com.mingle.event_service.exception.type;

public class ValidationException extends RuntimeException {
    public ValidationException(String message) {
        super(message);
    }
}
