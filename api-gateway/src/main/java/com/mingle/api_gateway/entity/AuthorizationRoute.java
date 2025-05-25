package com.mingle.api_gateway.entity;

import lombok.Data;

@Data
public class AuthorizationRoute {
    private String id;
    private String path;
    private String uri;
}
