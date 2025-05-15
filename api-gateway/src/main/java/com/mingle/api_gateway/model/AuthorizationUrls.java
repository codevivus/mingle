package com.mingle.api_gateway.model;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@Data
@ConfigurationProperties(prefix = "authorization")
public class AuthorizationUrls {
    private List<String> whitelistPaths;
    private List<AuthorizationRoute> routes;
    private CorsSettings cors;
}
