package com.mingle.api_gateway;

import com.mingle.api_gateway.entity.AuthorizationUrls;
import com.mingle.api_gateway.filter.InternalApiKeyWebFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = "CORS_ALLOWED_ORIGINS=https://app.example.com,https://admin.example.com")
@ActiveProfiles("test")
class StartupConfigTest {

    @DynamicPropertySource
    static void redis(DynamicPropertyRegistry registry) {
        RedisTestContainer.register(registry);
    }

    @Autowired AuthorizationUrls urls;

    @Test
    void corsOriginsComeFromTheEnvironment() {
        assertThat(urls.getCors().getAllowedOrigins())
                .containsExactly("https://app.example.com", "https://admin.example.com");
    }

    @Test
    void aShortInternalKeyStopsTheGatewayFromStarting() {
        assertThatThrownBy(() -> new InternalApiKeyWebFilter("short"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("INTERNAL_API_KEY");
        assertThatThrownBy(() -> new InternalApiKeyWebFilter(""))
                .isInstanceOf(IllegalStateException.class);
    }
}
