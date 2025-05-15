package com.mingle.a2srv_service.dto;

public record AuthRequest(
        String email,
        String password
) {}
