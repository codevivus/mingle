package com.mingle.api_gateway.entity;

import lombok.Data;

import java.util.List;

@Data
public class CorsSettings {
    private List<String> allowedOrigins;
    private List<String> allowedMethods;
    private List<String> allowedHeaders;
}
