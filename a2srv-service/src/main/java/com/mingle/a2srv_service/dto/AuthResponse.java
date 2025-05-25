package com.mingle.a2srv_service.dto;

public record AuthResponse(
        String token,
        String refreshToken,
        String userId,
        String username,
        boolean verified
) {}
