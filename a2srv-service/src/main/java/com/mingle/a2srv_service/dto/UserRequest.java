package com.mingle.a2srv_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserRequest {
    private String username;
    private String email;
    private String password;
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
    private LocalDate birthDate;
    private Float locationLat;
    private Float locationLon;
}
