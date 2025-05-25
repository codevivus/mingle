package com.mingle.ticket_service.client.dto.response;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class UserResponse {
    private Long id;
    private String username;
    private String name;
    private String surname;
    private Integer age;
    private String gender;
    private String bio;
    private String photoUrl;
    private String city;
    private List<String> interests;
    private String datePreference;
    private String relationshipGoal;
    private boolean online;
    private boolean verified;
    private LocalDate birthDate;
    private Float locationLat;
    private Float locationLon;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime lastActiveAt;
}
