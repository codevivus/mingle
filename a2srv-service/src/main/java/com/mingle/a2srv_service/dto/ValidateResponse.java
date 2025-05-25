package com.mingle.a2srv_service.dto;

import lombok.Data;

@Data
public class ValidateResponse {
    private boolean valid;
    private String userId;
}
