package com.mingle.ticket_service.exception.type;

import lombok.Getter;

import java.util.List;

@Getter
public class ValidationException extends RuntimeException {
    private final List<String> validationMessages;

    public ValidationException(List<String> validationMessages) {
        super("Validation failed");
        this.validationMessages = validationMessages;
    }

}
