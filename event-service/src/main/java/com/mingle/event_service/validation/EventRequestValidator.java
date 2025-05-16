package com.mingle.event_service.validation;

import com.mingle.event_service.dto.EventRequestDto;
import com.mingle.event_service.exception.type.ValidationException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDateTime;

@Component
public class EventRequestValidator {

    public void validate(EventRequestDto dto) {
        validateTitle(dto.title());
        validateDescription(dto.description());
        validateCity(dto.city());
        validateVenue(dto.venue());
        validateStartTime(dto.startTime());
        validateEndTime(dto.startTime(), dto.endTime());
        validateImageUrl(dto.imageUrl());
    }

    private void validateTitle(String title) {
        if (!StringUtils.hasText(title)) {
            throw new ValidationException("Event title must not be empty.");
        }
        if (title.length() < 5 || title.length() > 200) {
            throw new ValidationException("Event title must be between 5 and 200 characters.");
        }
    }

    private void validateDescription(String description) {
        if (description != null && description.length() > 1000) {
            throw new ValidationException("Event description must not exceed 1000 characters.");
        }
    }

    private void validateCity(String city) {
        if (!StringUtils.hasText(city)) {
            throw new ValidationException("City must not be empty.");
        }
        if (city.length() < 2 || city.length() > 100) {
            throw new ValidationException("City must be between 2 and 100 characters.");
        }
        if (!city.matches("^[A-Za-z\\s]+$")) {
            throw new ValidationException("City must contain only letters and spaces.");
        }
    }

    private void validateVenue(String venue) {
        if (!StringUtils.hasText(venue)) {
            throw new ValidationException("Venue must not be empty.");
        }
        if (venue.length() < 2 || venue.length() > 200) {
            throw new ValidationException("Venue must be between 2 and 200 characters.");
        }
    }

    private void validateStartTime(LocalDateTime startTime) {
        if (startTime == null) {
            throw new ValidationException("Start time is required.");
        }
        if (startTime.isBefore(LocalDateTime.now())) {
            throw new ValidationException("Start time cannot be in the past.");
        }
    }

    private void validateEndTime(LocalDateTime startTime, LocalDateTime endTime) {
        if (endTime != null && startTime != null && endTime.isBefore(startTime)) {
            throw new ValidationException("End time must be after start time.");
        }
    }

    private void validateImageUrl(String imageUrl) {
        if (imageUrl != null && !imageUrl.isBlank()) {
            try {
                new URL(imageUrl);
            } catch (MalformedURLException e) {
                throw new ValidationException("Image URL is not valid.");
            }
        }
    }
}
