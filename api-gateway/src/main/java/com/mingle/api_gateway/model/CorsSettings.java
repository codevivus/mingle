package com.mingle.api_gateway.model;

import lombok.Data;

import java.util.List;

@Data
public class CorsSettings {
    private List<String> allowedOrigins;
    private List<String> allowedMethods;
    private List<String> allowedHeaders;
}
