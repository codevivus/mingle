package com.mingle.api_gateway.filter;

import com.mingle.api_gateway.entity.AuthorizationUrls;
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

import java.util.Optional;
import java.util.UUID;

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

        final String requestId = Optional.ofNullable(
                        exchange.getRequest().getHeaders().getFirst("X-Request-ID"))
                .filter(id -> !id.isBlank())
                .orElse(UUID.randomUUID().toString());

        log.info("[GATEWAY][{}] Incoming request: {}", requestId, requestPath);

        if (authorizationUrls.getWhitelistPaths().stream().anyMatch(requestPath::startsWith)) {
            log.debug("[GATEWAY][{}] Whitelisted path", requestId);
            // Never let a client-supplied identity header reach a downstream service.
            ServerHttpRequest stripped = exchange.getRequest().mutate()
                    .headers(h -> {
                        h.remove("X-User-Id");
                        h.remove("X-User-Role");
                    })
                    .build();
            return chain.filter(exchange.mutate().request(stripped).build());
        }

        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("[GATEWAY][{}] Missing or malformed Authorization header", requestId);
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        String token = authHeader.substring(7);

        return tokenBlacklistService.isTokenBlacklisted(token)
                .flatMap(blacklisted -> {
                    if (blacklisted) {
                        log.warn("[GATEWAY][{}] Token is blacklisted", requestId);
                        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                        return exchange.getResponse().setComplete();
                    }

                    return tokenValidatorService.validateTokenReactive(token)
                            .flatMap(validated -> {
                                String userId = validated.getUserId();
                                log.info("[GATEWAY][{}] Token validated for userId: {}", requestId, userId);

                                ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                                        .header("X-User-Id", userId)
                                        .header("X-User-Role", validated.getRole() != null ? validated.getRole() : "USER")
                                        .header("X-Request-ID", requestId)
                                        .build();

                                return chain.filter(exchange.mutate().request(mutatedRequest).build());
                            })
                            .onErrorResume(ex -> {
                                log.error("[GATEWAY][{}] Token validation failed: {}", requestId, ex.getMessage());
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
