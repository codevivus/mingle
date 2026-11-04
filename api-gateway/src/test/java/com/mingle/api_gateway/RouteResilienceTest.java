package com.mingle.api_gateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class RouteResilienceTest {

    @DynamicPropertySource
    static void redis(DynamicPropertyRegistry registry) {
        RedisTestContainer.register(registry);
    }

    @LocalServerPort int port;
    @Autowired RouteLocator routeLocator;

    private static final List<String> RATE_LIMITED_ONLY = List.of("chat-websocket");
    private static final List<String> UNFILTERED = List.of("swagger-docs", "swagger-ui", "swagger-static");

    @Test
    void apiRoutesHaveRateLimiterAndCircuitBreaker() {
        var routes = routeLocator.getRoutes().collectList().block();
        assertThat(routes).isNotEmpty();

        for (var route : routes) {
            List<String> filters = route.getFilters().stream().map(Object::toString).toList();
            String joined = String.join(" ", filters);
            if (UNFILTERED.contains(route.getId())) {
                continue;
            }
            assertThat(joined).as("rate limiter on %s", route.getId()).contains("RateLimiter");
            if (!RATE_LIMITED_ONLY.contains(route.getId())) {
                assertThat(joined).as("circuit breaker on %s", route.getId()).containsIgnoringCase("CircuitBreaker");
            }
        }
    }

    @Test
    void downBackendAnswersFromFallbackThenRateLimitKicksIn() {
        WebTestClient client = WebTestClient.bindToServer().baseUrl("http://localhost:" + port).build();

        List<Integer> statuses = new ArrayList<>();
        for (int i = 0; i < 40; i++) {
            // /auth/login is whitelisted, and no a2srv-service is registered in this test.
            EntityExchangeResult<byte[]> result = client.post().uri("/auth/login").exchange()
                    .expectBody().returnResult();
            statuses.add(result.getStatus().value());
        }

        assertThat(statuses.get(0)).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE.value());
        assertThat(statuses).contains(HttpStatus.TOO_MANY_REQUESTS.value());
    }
}
