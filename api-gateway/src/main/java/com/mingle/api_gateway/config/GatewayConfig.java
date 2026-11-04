package com.mingle.api_gateway.config;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class GatewayConfig {

    /**
     * Resolves service names through Eureka. Without it, http://a2srv-service/... goes to port 80 of
     * the container and token validation never reaches the auth service.
     */
    @Bean
    @LoadBalanced
    public WebClient.Builder loadBalancedWebClientBuilder() {
        return WebClient.builder();
    }

    @Bean
    public WebClient webClient(@LoadBalanced WebClient.Builder builder) {
        return builder.build();
    }
}
