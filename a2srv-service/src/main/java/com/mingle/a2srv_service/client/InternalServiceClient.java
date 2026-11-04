package com.mingle.a2srv_service.client;

import com.mingle.a2srv_service.exception.type.DependencyUnavailableException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Calls the /internal endpoints of the other services (the gateway does not route them) with the shared key.
 * Every failure surfaces as {@link DependencyUnavailableException} so callers can stop before deleting anything.
 */
@Component
@RequiredArgsConstructor
public class InternalServiceClient {

    private static final Duration TIMEOUT = Duration.ofSeconds(15);
    private static final ParameterizedTypeReference<Map<String, Object>> MAP = new ParameterizedTypeReference<>() { };
    private static final ParameterizedTypeReference<List<Map<String, Object>>> LIST = new ParameterizedTypeReference<>() { };

    private final WebClient webClient;

    @Value("${gateway.internal-api-key}")
    private String internalApiKey;

    public void purgeMatchData(Long userId) {
        delete("match-service", userId);
    }

    public void purgeTicketData(Long userId) {
        delete("ticket-service", userId);
    }

    public Map<String, Object> exportMatchData(Long userId) {
        return call("match-service", () -> webClient.get()
                .uri("http://match-service/internal/users/{id}/export", userId)
                .header("X-Internal-Key", internalApiKey)
                .retrieve().bodyToMono(MAP).block(TIMEOUT));
    }

    public List<Map<String, Object>> exportTickets(Long userId) {
        return call("ticket-service", () -> webClient.get()
                .uri("http://ticket-service/internal/users/{id}/tickets", userId)
                .header("X-Internal-Key", internalApiKey)
                .retrieve().bodyToMono(LIST).block(TIMEOUT));
    }

    private void delete(String service, Long userId) {
        call(service, () -> webClient.delete()
                .uri("http://" + service + "/internal/users/{id}", userId)
                .header("X-Internal-Key", internalApiKey)
                .retrieve().toBodilessEntity().block(TIMEOUT));
    }

    private <T> T call(String service, Supplier<T> request) {
        try {
            return request.get();
        } catch (RuntimeException e) {
            throw new DependencyUnavailableException(service + " did not answer", e);
        }
    }
}
