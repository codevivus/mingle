package com.mingle.event_service.client;

import com.mingle.event_service.config.TicketmasterProperties;
import com.mingle.event_service.dto.ticketmaster.TicketmasterEventDto;
import com.mingle.event_service.dto.ticketmaster.response.TicketmasterVenueResponseWrapper;
import com.mingle.event_service.exception.type.RateLimitException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class TicketmasterClient {

    private final WebClient webClient;
    private final TicketmasterProperties properties;

    public List<TicketmasterEventDto> fetchEventsByCity(String city) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/events.json")
                        .queryParam("city", city)
                        .queryParam("size", properties.getSize())
                        .queryParam("apikey", properties.getKey())
                        .build())
                .retrieve()
                .onStatus(
                        status -> status.value() == 429,
                        response -> {
                            log.warn("Rate limit exceeded for city: {}", city);
                            var retryAfter = response.headers().asHttpHeaders().getFirst("Retry-After");
                            int delaySeconds = properties.getRetryDefaultDelaySeconds();
                            if (retryAfter != null) {
                                try {
                                    delaySeconds = Integer.parseInt(retryAfter);
                                } catch (NumberFormatException ignored) {
                                }
                            }
                            Duration delay = Duration.ofSeconds(delaySeconds);
                            return reactor.core.publisher.Mono.error(new RateLimitException("Rate limit hit, retry after " + delaySeconds + "s", delay));
                        }
                )
                .onStatus(
                        HttpStatusCode::is5xxServerError,
                        response -> {
                            log.error("Server error while fetching events for city: {}", city);
                            return response.createException();
                        }
                )
                .bodyToMono(TicketmasterVenueResponseWrapper.class)
                .map(TicketmasterVenueResponseWrapper::extractEvents)
                .retryWhen(Retry.fixedDelay(properties.getRetryMaxAttempts(), Duration.ofSeconds(properties.getRetryDefaultDelaySeconds()))
                        .filter(throwable -> {
                            if (throwable instanceof RateLimitException) {
                                try {
                                    Thread.sleep(((RateLimitException) throwable).getRetryDelay().toMillis());
                                } catch (InterruptedException ignored) {
                                }
                                return true;
                            }
                            return false;
                        })
                )
                .block();
    }
}
