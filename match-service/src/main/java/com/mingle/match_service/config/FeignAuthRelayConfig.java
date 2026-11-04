package com.mingle.match_service.config;

import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Headers every outgoing Feign call carries: the caller bearer token when one is being served (a2srv-service
 * requires a JWT on /user/{id}) and the shared internal key (ticket-service guards its /internal endpoints).
 */
@Configuration
public class FeignAuthRelayConfig {

    @Bean
    public RequestInterceptor authorizationRelayInterceptor(@Value("${internal.api-key:}") String internalKey) {
        return template -> {
            template.header("X-Internal-Key", internalKey);
            if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
                String authorization = attributes.getRequest().getHeader(HttpHeaders.AUTHORIZATION);
                if (authorization != null && !authorization.isBlank()) {
                    template.header(HttpHeaders.AUTHORIZATION, authorization);
                }
            }
        };
    }
}
