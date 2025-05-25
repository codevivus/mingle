package com.mingle.api_gateway.config;

import com.mingle.api_gateway.model.AuthorizationRoute;
import com.mingle.api_gateway.model.AuthorizationUrls;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class RouteLocatorConfig {

    private final AuthorizationUrls authorizationUrls;

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        var routesBuilder = builder.routes();

        for (AuthorizationRoute route : authorizationUrls.getRoutes()) {
            routesBuilder.route(route.getId(), r -> r
                    .path(route.getPath())
                    .uri(route.getUri()));
        }

        return routesBuilder.build();
    }
}
