package com.mingle.event_service.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "events")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String externalId;

    private String title;

    private String description;

    private String city;

    private String venue;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private String imageUrl;

    private boolean imported;
}
