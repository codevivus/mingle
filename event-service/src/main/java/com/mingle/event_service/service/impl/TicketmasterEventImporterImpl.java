package com.mingle.event_service.service.impl;

import com.mingle.event_service.client.TicketmasterClient;
import com.mingle.event_service.dto.ticketmaster.TicketmasterEventDto;
import com.mingle.event_service.exception.type.ExternalApiException;
import com.mingle.event_service.mapper.TicketmasterEventManualMapper;
import com.mingle.event_service.model.Event;
import com.mingle.event_service.repository.EventRepository;
import com.mingle.event_service.service.TicketmasterEventImporter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TicketmasterEventImporterImpl implements TicketmasterEventImporter {

    private final TicketmasterClient client;
    private final TicketmasterEventManualMapper mapper;
    private final EventRepository repository;

    @Override
    public void importEvents(String city) {
        List<TicketmasterEventDto> dtos;

        try {
            dtos = client.fetchEventsByCity(city);
        } catch (Exception e) {
            throw new ExternalApiException("Failed to fetch events from Ticketmaster for city: " + city, e);
        }

        for (TicketmasterEventDto dto : dtos) {
            boolean exists = repository.findByExternalId(dto.getId()).isPresent();
            if (exists) continue;

            try {
                Event event = mapper.toEntity(dto);
                repository.save(event);
            } catch (Exception e) {
                throw new ExternalApiException("Error while mapping/saving Ticketmaster event with ID: " + dto.getId(), e);
            }
        }
    }
}
