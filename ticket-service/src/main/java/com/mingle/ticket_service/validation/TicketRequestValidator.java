package com.mingle.ticket_service.validation;

import com.mingle.ticket_service.dto.TicketRequest;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

@Component
public class TicketRequestValidator implements Validator {

    private static final int MAX_IMAGE_SIZE_BYTES = 5 * 1024 * 1024;

    @Override
    public boolean supports(Class<?> clazz) {
        return TicketRequest.class.isAssignableFrom(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        TicketRequest request = (TicketRequest) target;

        if (request.getEventId() == null || request.getEventId() <= 0) {
            errors.rejectValue("eventId", "Invalid.eventId", "Event ID must be a positive number");
        }

        if (request.getUserId() == null || request.getUserId() <= 0) {
            errors.rejectValue("userId", "Invalid.userId", "User ID must be a positive number");
        }

        if (request.getImageData() != null) {
            if (request.getImageData().length == 0) {
                errors.rejectValue("imageData", "Empty.imageData", "Image data cannot be empty");
            } else if (request.getImageData().length > MAX_IMAGE_SIZE_BYTES) {
                errors.rejectValue("imageData", "TooLarge.imageData", "Image data exceeds 5MB limit");
            }
        }
    }
}
