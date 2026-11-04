package com.mingle.api_gateway;

import com.mingle.api_gateway.service.TokenBlacklistService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class InternalEndpointProtectionTest {

    @DynamicPropertySource
    static void redis(DynamicPropertyRegistry registry) {
        RedisTestContainer.register(registry);
    }

    @LocalServerPort int port;
    @Autowired TokenBlacklistService blacklist;

    private WebTestClient client() {
        return WebTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
    }

    @Test
    void rejectsRequestWithoutKey() {
        client().post().uri("/internal/token/blacklist?exp=60")
                .header("Authorization", "Bearer victim-token-1")
                .exchange()
                .expectStatus().isForbidden();

        assertThat(blacklist.isTokenBlacklisted("victim-token-1").block()).isFalse();
    }

    @Test
    void rejectsRequestWithWrongKey() {
        client().post().uri("/internal/token/blacklist?exp=60")
                .header("Authorization", "Bearer victim-token-2")
                .header("X-Internal-Key", "nope")
                .exchange()
                .expectStatus().isForbidden();

        assertThat(blacklist.isTokenBlacklisted("victim-token-2").block()).isFalse();
    }

    @Test
    void acceptsRequestWithCorrectKey() {
        client().post().uri("/internal/token/blacklist?exp=60")
                .header("Authorization", "Bearer good-token-3")
                .header("X-Internal-Key", "test-internal-key")
                .exchange()
                .expectStatus().isOk();

        assertThat(blacklist.isTokenBlacklisted("good-token-3").block()).isTrue();
    }
}
