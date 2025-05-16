package com.mingle.event_service.service.impl;

import com.mingle.event_service.config.TicketmasterProperties;
import com.mingle.event_service.dto.ticketmaster.TicketmasterCityDto;
import com.mingle.event_service.dto.ticketmaster.TicketmasterVenueDto;
import com.mingle.event_service.dto.ticketmaster.response.TicketmasterPageDto;
import com.mingle.event_service.dto.ticketmaster.response.TicketmasterVenueResponseWrapper;
import com.mingle.event_service.service.TicketmasterCityDiscoveryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TicketmasterCityDiscoveryServiceImpl implements TicketmasterCityDiscoveryService {

    private final WebClient webClient;
    private final TicketmasterProperties properties;

    @Override
    public List<String> getAvailableCities() {
        Set<String> cities = new HashSet<>();
        AtomicInteger page = new AtomicInteger(0);
        int totalPages = 1;

        while (page.get() < totalPages) {
            try {
                TicketmasterVenueResponseWrapper response = webClient.get()
                        .uri(uriBuilder -> uriBuilder
                                .path("/venues.json")
                                .queryParam("countryCode", "TR")
                                .queryParam("size", properties.getSize())
                                .queryParam("page", page.get())
                                .queryParam("apikey", properties.getKey())
                                .build())
                        .retrieve()
                        .bodyToMono(TicketmasterVenueResponseWrapper.class)
                        .block();

                if (response == null || response.getEmbedded() == null) break;

                totalPages = Optional.ofNullable(response.getPage())
                        .map(TicketmasterPageDto::getTotalPages)
                        .orElse(1);

                response.getEmbedded().getVenues().stream()
                        .map(TicketmasterVenueDto::getCity)  // lambda kullanıldı
                        .filter(Objects::nonNull)
                        .map(TicketmasterCityDto::getName)
                        .filter(name -> name != null && !name.isBlank())
                        .map(String::trim)
                        .forEach(cities::add);

                page.incrementAndGet();

            } catch (Exception e) {
                log.warn("Failed to fetch cities from Ticketmaster (page {}): {}", page.get(), e.getMessage());
                break;
            }
        }

        return cities.stream().sorted().collect(Collectors.toList());
    }
}
