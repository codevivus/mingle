package com.mingle.api_gateway.entity;

import lombok.Data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
public class AuthorizationRoute {
    private String id;
    private String path;
    private String uri;
    private List<FilterSpec> filters = new ArrayList<>();

    /** A gateway filter by its factory name, e.g. RequestRateLimiter or CircuitBreaker, with its arguments. */
    @Data
    public static class FilterSpec {
        private String name;
        private Map<String, String> args = new LinkedHashMap<>();
    }
}
