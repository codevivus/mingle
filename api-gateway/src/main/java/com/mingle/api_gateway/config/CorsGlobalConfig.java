package com.mingle.api_gateway.config;

import com.mingle.api_gateway.model.AuthorizationUrls;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

@Configuration
@RequiredArgsConstructor
public class CorsGlobalConfig {

    private final AuthorizationUrls authorizationUrls;

    @Bean
    public CorsWebFilter corsWebFilter() {
        var config = new CorsConfiguration();
        config.setAllowCredentials(true);

        config.setAllowedOrigins(authorizationUrls.getCors().getAllowedOrigins());
        config.setAllowedMethods(authorizationUrls.getCors().getAllowedMethods());
        config.setAllowedHeaders(authorizationUrls.getCors().getAllowedHeaders());

        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return new CorsWebFilter(source);
    }
}
