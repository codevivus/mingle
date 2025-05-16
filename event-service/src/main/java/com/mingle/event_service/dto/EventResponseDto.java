package com.mingle.event_service.dto;

import java.time.LocalDateTime;

public record EventResponseDto(
        Long id,
        String title,
        String description,
        String city,
        String venue,
        LocalDateTime startTime,
        LocalDateTime endTime,
        String imageUrl
) {}
