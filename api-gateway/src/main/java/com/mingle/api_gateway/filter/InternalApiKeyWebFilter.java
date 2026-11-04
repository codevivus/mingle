package com.mingle.api_gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Guards the gateway's own /internal/** endpoints (e.g. the token blacklist). They are called by
 * other services, never by end users, so they need a shared key instead of a user JWT. With no key
 * configured every request is rejected.
 */
@Slf4j
@Component
public class InternalApiKeyWebFilter implements WebFilter, Ordered {

    static final String HEADER = "X-Internal-Key";

    private final byte[] expectedKey;

    public InternalApiKeyWebFilter(@Value("${internal.api-key:}") String expectedKey) {
        this.expectedKey = expectedKey.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        if (!exchange.getRequest().getPath().value().startsWith("/internal/")) {
            return chain.filter(exchange);
        }
        String provided = exchange.getRequest().getHeaders().getFirst(HEADER);
        if (expectedKey.length == 0 || provided == null
                || !MessageDigest.isEqual(expectedKey, provided.getBytes(StandardCharsets.UTF_8))) {
            log.warn("[GATEWAY] Rejected internal request to {}", exchange.getRequest().getPath());
            exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
            return exchange.getResponse().setComplete();
        }
        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
