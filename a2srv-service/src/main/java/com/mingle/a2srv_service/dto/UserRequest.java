package com.mingle.a2srv_service.dto;

import java.time.LocalDate;
import java.util.List;

public record UserRequest(
        String username,
        String email,
        String password,
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
        LocalDate birthDate,
        Float locationLat,
        Float locationLon
) {}
