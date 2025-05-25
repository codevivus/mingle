package com.mingle.api_gateway.config;

import com.mingle.api_gateway.entity.AuthorizationUrls;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource(value = "classpath:authorization.yaml", factory = YamlPropertySourceFactory.class)
@EnableConfigurationProperties(AuthorizationUrls.class)
public class GatewayRoutesConfig {
}
