package com.mingle.api_gateway.entity;

import lombok.Data;

@Data
public class ValidateResponse {
    private boolean valid;
    private String userId;
}
