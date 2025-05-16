package com.mingle.event_service.dto;

import java.time.LocalDateTime;

public record EventRequestDto(
        String title,
        String description,
        String city,
        String venue,
        LocalDateTime startTime,
        LocalDateTime endTime,
        String imageUrl
) {}
