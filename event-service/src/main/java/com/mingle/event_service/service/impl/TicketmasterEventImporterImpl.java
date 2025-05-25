package com.mingle.event_service.service.impl;

import com.mingle.event_service.client.TicketmasterClient;
import com.mingle.event_service.dto.ticketmaster.TicketmasterEventDto;
import com.mingle.event_service.exception.type.ExternalApiException;
import com.mingle.event_service.mapper.TicketmasterEventManualMapper;
import com.mingle.event_service.model.Event;
import com.mingle.event_service.repository.EventRepository;
import com.mingle.event_service.service.TicketmasterEventImporter;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TicketmasterEventImporterImpl implements TicketmasterEventImporter {

    private final TicketmasterClient client;
    private final TicketmasterEventManualMapper mapper;
    private final EventRepository repository;

    @Override
    @Transactional
    public void importEvents(String city) {
        List<TicketmasterEventDto> dtos;
        try {
            dtos = client.fetchEventsByCity(city);
            log.info("Fetched {} events from Ticketmaster for city '{}'", dtos.size(), city);
        } catch (Exception e) {
            throw new ExternalApiException("Failed to fetch events from Ticketmaster for city: " + city, e);
        }

        if (dtos.isEmpty()) {
            log.info("No events found from Ticketmaster for city '{}'", city);
            return;
        }

        Set<String> existingIds = repository.findAllByExternalIdIn(
                dtos.stream().map(TicketmasterEventDto::getId).toList()
        ).stream().map(Event::getExternalId).collect(Collectors.toSet());

        List<Event> eventsToSave = new ArrayList<>();

        for (TicketmasterEventDto dto : dtos) {
            if (existingIds.contains(dto.getId())) continue;

            try {
                Event event = mapper.toEntity(dto);
                if (event != null) {
                    eventsToSave.add(event);
                } else {
                    log.warn("Mapper returned null for event ID: {}", dto.getId());
                }
            } catch (Exception e) {
                log.warn("Failed to map/save event ID: {}", dto.getId(), e);
            }
        }

        if (eventsToSave.isEmpty()) {
            log.info("No new events to save for city '{}'", city);
            return;
        }

        repository.saveAll(eventsToSave);
        log.info("Successfully saved {} new events for city '{}'", eventsToSave.size(), city);
    }
}
