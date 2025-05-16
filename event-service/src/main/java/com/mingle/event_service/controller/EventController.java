package com.mingle.event_service.controller;

import com.mingle.event_service.dto.EventRequestDto;
import com.mingle.event_service.dto.EventResponseDto;
import com.mingle.event_service.service.EventService;
import com.mingle.event_service.service.TicketmasterCityDiscoveryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;
    private final TicketmasterCityDiscoveryService cityDiscoveryService;


    @GetMapping
    public ResponseEntity<List<EventResponseDto>> getAllEvents() {
        return ResponseEntity.ok(eventService.getAllEvents());
    }

    @GetMapping("/{id}")
    public ResponseEntity<EventResponseDto> getEventById(@PathVariable Long id) {
        return ResponseEntity.ok(eventService.getEventById(id));
    }

    @PostMapping
    public ResponseEntity<EventResponseDto> createEvent(@RequestBody EventRequestDto dto) {
        return new ResponseEntity<>(eventService.createEvent(dto), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<EventResponseDto> updateEvent(@PathVariable Long id,
                                                        @RequestBody EventRequestDto dto) {
        return ResponseEntity.ok(eventService.updateEvent(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEvent(@PathVariable Long id) {
        eventService.deleteEvent(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/import")
    public ResponseEntity<Void> importEvents(@RequestParam String city) {
        eventService.importEventsFromTicketmaster(city);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/cities")
    public ResponseEntity<List<String>> getAvailableCities() {
        List<String> cities = cityDiscoveryService.getAvailableCities();
        return ResponseEntity.ok(cities);
    }
}
