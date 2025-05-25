package com.mingle.event_service.controller;

import com.mingle.event_service.dto.EventRequestDto;
import com.mingle.event_service.dto.EventResponseDto;
import com.mingle.event_service.service.EventService;
import com.mingle.event_service.service.TicketmasterCityDiscoveryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/event")
@RequiredArgsConstructor
@Tag(name = "Event", description = "Event management and Ticketmaster integration endpoints")
public class EventController {

    private final EventService eventService;
    private final TicketmasterCityDiscoveryService cityDiscoveryService;

    @Operation(summary = "Get all events", description = "Returns a list of all events")
    @ApiResponse(responseCode = "200", description = "Events fetched successfully")
    @GetMapping
    public ResponseEntity<List<EventResponseDto>> getAllEvents() {
        return ResponseEntity.ok(eventService.getAllEvents());
    }

    @Operation(summary = "Get event by ID", description = "Returns a single event by its ID")
    @ApiResponse(responseCode = "200", description = "Event found and returned")
    @GetMapping("/{id}")
    public ResponseEntity<EventResponseDto> getEventById(
            @Parameter(description = "ID of the event", required = true)
            @PathVariable Long id) {
        return ResponseEntity.ok(eventService.getEventById(id));
    }

    @Operation(summary = "Create a new event", description = "Creates a new event record")
    @ApiResponse(responseCode = "201", description = "Event created successfully")
    @PostMapping
    public ResponseEntity<EventResponseDto> createEvent(
            @Parameter(description = "Event request body", required = true)
            @RequestBody EventRequestDto dto) {
        return new ResponseEntity<>(eventService.createEvent(dto), HttpStatus.CREATED);
    }

    @Operation(summary = "Update an existing event", description = "Updates the event with the given ID")
    @ApiResponse(responseCode = "200", description = "Event updated successfully")
    @PutMapping("/{id}")
    public ResponseEntity<EventResponseDto> updateEvent(
            @Parameter(description = "ID of the event to update", required = true)
            @PathVariable Long id,
            @Parameter(description = "Updated event data", required = true)
            @RequestBody EventRequestDto dto) {
        return ResponseEntity.ok(eventService.updateEvent(id, dto));
    }

    @Operation(summary = "Delete an event", description = "Deletes the event with the given ID")
    @ApiResponse(responseCode = "204", description = "Event deleted successfully")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEvent(
            @Parameter(description = "ID of the event to delete", required = true)
            @PathVariable Long id) {
        eventService.deleteEvent(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Import events from Ticketmaster", description = "Imports external events for the specified city")
    @ApiResponse(responseCode = "200", description = "Events imported successfully")
    @PostMapping("/import")
    public ResponseEntity<Void> importEvents(
            @Parameter(description = "City to import events from", required = true)
            @RequestParam String city) {
        eventService.importEventsFromTicketmaster(city);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Get available Ticketmaster cities", description = "Returns a list of cities for which event data can be imported")
    @ApiResponse(responseCode = "200", description = "Cities fetched successfully")
    @GetMapping("/cities")
    public ResponseEntity<List<String>> getAvailableCities() {
        List<String> cities = cityDiscoveryService.getAvailableCities();
        return ResponseEntity.ok(cities);
    }
}
