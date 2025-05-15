package com.mingle.api_gateway.model;

import lombok.Data;

@Data
public class AuthorizationRoute {
    private String id;
    private String path;
    private String uri;
}
