package com.mingle.a2srv_service.dto;
import lombok.Data;

@Data
public class AuthResponse {
    private String token;
    private String refreshToken;
    private String userId;
    private String username;
    private boolean verified;
}
