package com.mingle.event_service.service.impl;

import com.mingle.event_service.client.TicketmasterClient;
import com.mingle.event_service.dto.EventRequestDto;
import com.mingle.event_service.dto.EventResponseDto;
import com.mingle.event_service.dto.ticketmaster.TicketmasterEventDto;
import com.mingle.event_service.exception.type.ExternalApiException;
import com.mingle.event_service.mapper.EventMapper;
import com.mingle.event_service.mapper.TicketmasterEventManualMapper;
import com.mingle.event_service.model.Event;
import com.mingle.event_service.repository.EventRepository;
import com.mingle.event_service.service.EventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;
    private final EventMapper eventMapper;
    private final TicketmasterClient ticketmasterClient;
    private final TicketmasterEventManualMapper ticketmasterMapper;

    @Override
    public List<EventResponseDto> getAllEvents() {
        return eventMapper.toDtoList(eventRepository.findAll());
    }

    @Override
    public EventResponseDto getEventById(Long id) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Event not found"));
        return eventMapper.toDto(event);
    }

    @Override
    public EventResponseDto createEvent(EventRequestDto dto) {
        Event event = eventMapper.toEntity(dto);
        return eventMapper.toDto(eventRepository.save(event));
    }

    @Override
    public EventResponseDto updateEvent(Long id, EventRequestDto dto) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Event not found"));
        eventMapper.updateEventFromDto(dto, event);
        return eventMapper.toDto(eventRepository.save(event));
    }

    @Override
    public void deleteEvent(Long id) {
        eventRepository.deleteById(id);
    }

    @Override
    @Transactional
    public void importEventsFromTicketmaster(String city) {
        List<TicketmasterEventDto> dtos;
        try {
            dtos = ticketmasterClient.fetchEventsByCity(city);
            log.info("Fetched {} events from Ticketmaster for city '{}'", dtos.size(), city);
        } catch (Exception e) {
            throw new ExternalApiException("Failed to fetch events from Ticketmaster for city: " + city, e);
        }

        if (dtos.isEmpty()) {
            log.info("No events found from Ticketmaster for city '{}'", city);
            return;
        }

        Set<String> existingIds = eventRepository.findAllByExternalIdIn(
                dtos.stream().map(TicketmasterEventDto::getId).toList()
        ).stream().map(Event::getExternalId).collect(Collectors.toSet());

        List<Event> eventsToSave = dtos.stream()
                .filter(dto -> !existingIds.contains(dto.getId()))
                .map(ticketmasterMapper::toEntity)
                .filter(Objects::nonNull)
                .toList();

        if (eventsToSave.isEmpty()) {
            log.info("No new events to save for city '{}'", city);
            return;
        }

        eventRepository.saveAll(eventsToSave);
        log.info("Saved {} new events for city '{}'", eventsToSave.size(), city);
    }
}
