package com.mingle.api_gateway.config;

import com.mingle.api_gateway.entity.AuthorizationRoute;
import com.mingle.api_gateway.entity.AuthorizationUrls;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.filter.FilterDefinition;
import org.springframework.cloud.gateway.handler.predicate.PredicateDefinition;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Flux;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Builds the routes from authorization.yaml as route definitions, so the filters listed there
 * (RequestRateLimiter, CircuitBreaker, ...) are resolved to real gateway filters.
 */
@Configuration
@RequiredArgsConstructor
public class RouteLocatorConfig {

    private final AuthorizationUrls authorizationUrls;

    @Bean
    public RouteDefinitionLocator authorizationRouteDefinitionLocator() {
        List<RouteDefinition> definitions = authorizationUrls.getRoutes().stream()
                .map(this::toDefinition)
                .toList();
        return () -> Flux.fromIterable(definitions);
    }

    private RouteDefinition toDefinition(AuthorizationRoute route) {
        RouteDefinition definition = new RouteDefinition();
        definition.setId(route.getId());
        definition.setUri(URI.create(route.getUri()));
        definition.setPredicates(List.of(new PredicateDefinition("Path=" + route.getPath())));
        definition.setFilters(route.getFilters().stream().map(spec -> {
            FilterDefinition filter = new FilterDefinition();
            filter.setName(spec.getName());
            filter.setArgs(new LinkedHashMap<>(spec.getArgs()));
            return filter;
        }).toList());
        return definition;
    }
}
