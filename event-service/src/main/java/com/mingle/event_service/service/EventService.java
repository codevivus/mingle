package com.mingle.event_service.service;

import com.mingle.event_service.dto.EventRequestDto;
import com.mingle.event_service.dto.EventResponseDto;

import java.util.List;

public interface EventService {
    List<EventResponseDto> getAllEvents();
    EventResponseDto getEventById(Long id);
    EventResponseDto createEvent(EventRequestDto dto);
    EventResponseDto updateEvent(Long id, EventRequestDto dto);
    void deleteEvent(Long id);
    void importEventsFromTicketmaster(String city);

}
