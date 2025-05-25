package com.mingle.event_service.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class EventResponseDto {
    private Long id;
    private String title;
    private String description;
    private String city;
    private String venue;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String imageUrl;
}
