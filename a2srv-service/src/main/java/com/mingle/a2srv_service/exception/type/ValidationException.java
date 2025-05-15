package com.mingle.a2srv_service.exception.type;

public class ValidationException extends RuntimeException {
    public ValidationException(String messages) {
        super(messages);
    }
}
