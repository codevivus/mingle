package com.mingle.event_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EventRequestDto {
    private String title;
    private String description;
    private String city;
    private String venue;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String imageUrl;
}
