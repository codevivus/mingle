package com.mingle.api_gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;

@Configuration
public class RateLimiterConfig {

    /**
     * Limits per authenticated user (X-User-Id is set by JwtTokenAuthenticationFilter before route
     * filters run) and falls back to the client address for anonymous routes such as login.
     * The default resolver keys on the Spring Security principal, which is always empty here,
     * so without this bean every request would be rejected.
     */
    @Bean
    public KeyResolver userOrIpKeyResolver() {
        return exchange -> {
            String userId = exchange.getRequest().getHeaders().getFirst("X-User-Id");
            if (userId != null && !userId.isBlank()) {
                return Mono.just("user:" + userId);
            }
            InetSocketAddress address = exchange.getRequest().getRemoteAddress();
            return Mono.just("ip:" + (address != null && address.getAddress() != null
                    ? address.getAddress().getHostAddress() : "unknown"));
        };
    }
}
