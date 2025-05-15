package com.mingle.api_gateway.model;

import lombok.Data;

@Data
public class ValidateResponse {
    private boolean valid;
    private String userId;
}
