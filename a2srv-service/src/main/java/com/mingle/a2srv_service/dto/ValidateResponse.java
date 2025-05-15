package com.mingle.a2srv_service.dto;

public record ValidateResponse(
        boolean valid,
        String userId
) {}

