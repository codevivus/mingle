package com.mingle.a2srv_service.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record UserResponse(
        Long id,
        String username,
        String name,
        String surname,
        Integer age,
        String gender,
        String bio,
        String photoUrl,
        String city,
        List<String> interests,
        String datePreference,
        String relationshipGoal,
        boolean online,
        boolean verified,
        LocalDate birthDate,
        Float locationLat,
        Float locationLon,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime lastActiveAt
) {}
