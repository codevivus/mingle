package com.mingle.ticket_service.controller;

import com.mingle.ticket_service.dto.TicketRequest;
import com.mingle.ticket_service.dto.TicketResponse;
import com.mingle.ticket_service.service.TicketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/ticket")
@RequiredArgsConstructor
@Tag(name = "Ticket", description = "Operations related to ticket management")
public class TicketController {

    private final TicketService ticketService;

    @Operation(summary = "Create a new ticket", description = "Creates a ticket for a given user and event")
    @ApiResponse(responseCode = "200", description = "Ticket successfully created")
    @PostMapping("/add")
    public ResponseEntity<TicketResponse> add(@Valid @RequestBody TicketRequest request) {
        return ResponseEntity.ok(ticketService.add(request));
    }

    @Operation(summary = "Get ticket by ID", description = "Retrieves a ticket by its unique ID")
    @ApiResponse(responseCode = "200", description = "Ticket retrieved successfully")
    @GetMapping("/{id}")
    public ResponseEntity<TicketResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ticketService.getById(id));
    }

    @Operation(summary = "Get tickets by user ID", description = "Retrieves all tickets belonging to a specific user")
    @ApiResponse(responseCode = "200", description = "Tickets retrieved successfully")
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<TicketResponse>> getByUserId(@PathVariable Long userId) {
        return ResponseEntity.ok(ticketService.getByUserId(userId));
    }

    @Operation(summary = "Get tickets by event ID", description = "Retrieves all tickets for a specific event")
    @ApiResponse(responseCode = "200", description = "Tickets retrieved successfully")
    @GetMapping("/event/{eventId}")
    public ResponseEntity<List<TicketResponse>> getByEventId(@PathVariable Long eventId) {
        return ResponseEntity.ok(ticketService.getByEventId(eventId));
    }

    @Operation(summary = "Get all tickets", description = "Returns a list of all tickets in the system")
    @ApiResponse(responseCode = "200", description = "All tickets retrieved successfully")
    @GetMapping
    public ResponseEntity<List<TicketResponse>> getAll() {
        return ResponseEntity.ok(ticketService.getAll());
    }

    @Operation(summary = "Delete a ticket", description = "Deletes a ticket by its ID")
    @ApiResponse(responseCode = "204", description = "Ticket successfully deleted")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        ticketService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
