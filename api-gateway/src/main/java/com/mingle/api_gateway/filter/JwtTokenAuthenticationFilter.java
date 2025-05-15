package com.mingle.api_gateway.filter;

import com.mingle.api_gateway.model.AuthorizationUrls;
import com.mingle.api_gateway.service.TokenBlacklistService;
import com.mingle.api_gateway.service.TokenValidatorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenAuthenticationFilter implements GlobalFilter, Ordered {

    private final AuthorizationUrls authorizationUrls;
    private final TokenValidatorService tokenValidatorService;
    private final TokenBlacklistService tokenBlacklistService;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String requestPath = exchange.getRequest().getPath().toString();

        if (authorizationUrls.getWhitelistPaths().stream().anyMatch(requestPath::startsWith)) {
            return chain.filter(exchange);
        }

        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        String token = authHeader.substring(7);

        return tokenBlacklistService.isTokenBlacklisted(token)
                .flatMap(blacklisted -> {
                    if (blacklisted) {
                        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                        return exchange.getResponse().setComplete();
                    }

                    return tokenValidatorService.validateTokenReactive(token)
                            .flatMap(userId -> {
                                ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                                        .header("X-User-Id", userId)
                                        .build();
                                return chain.filter(exchange.mutate().request(mutatedRequest).build());
                            })
                            .onErrorResume(ex -> {
                                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                                return exchange.getResponse().setComplete();
                            });
                });
    }

    @Override
    public int getOrder() {
        return -1;
    }
}

