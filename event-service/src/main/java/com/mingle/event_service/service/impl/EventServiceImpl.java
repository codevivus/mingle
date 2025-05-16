package com.mingle.event_service.service.impl;

import com.mingle.event_service.dto.EventRequestDto;
import com.mingle.event_service.dto.EventResponseDto;
import com.mingle.event_service.exception.type.NotFoundException;
import com.mingle.event_service.mapper.EventMapper;
import com.mingle.event_service.model.Event;
import com.mingle.event_service.repository.EventRepository;
import com.mingle.event_service.service.EventService;
import com.mingle.event_service.service.TicketmasterEventImporter;
import com.mingle.event_service.validation.EventRequestValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;
    private final EventMapper eventMapper;
    private final TicketmasterEventImporter ticketmasterEventImporter;
    private final EventRequestValidator eventRequestValidator;

    @Override
    public List<EventResponseDto> getAllEvents() {
        return eventMapper.toDtoList(eventRepository.findAll());
    }

    @Override
    public EventResponseDto getEventById(Long id) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Event not found with id: " + id));
        return eventMapper.toDto(event);
    }

    @Override
    public EventResponseDto createEvent(EventRequestDto dto) {
        eventRequestValidator.validate(dto);
        Event event = eventMapper.toEntity(dto);
        event.setImported(false);
        return eventMapper.toDto(eventRepository.save(event));
    }

    @Override
    public EventResponseDto updateEvent(Long id, EventRequestDto dto) {
        eventRequestValidator.validate(dto);
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Event not found with id: " + id));
        eventMapper.updateEventFromDto(dto, event);
        return eventMapper.toDto(eventRepository.save(event));
    }

    @Override
    public void deleteEvent(Long id) {
        if (!eventRepository.existsById(id)) {
            throw new NotFoundException("Event not found with id: " + id);
        }
        eventRepository.deleteById(id);
    }

    @Override
    public void importEventsFromTicketmaster(String city) {
        ticketmasterEventImporter.importEvents(city);
    }
}
