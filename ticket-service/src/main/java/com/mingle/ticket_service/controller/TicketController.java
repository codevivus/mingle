package com.mingle.ticket_service.controller;

import com.mingle.ticket_service.dto.TicketRequest;
import com.mingle.ticket_service.dto.TicketResponse;
import com.mingle.ticket_service.exception.type.ForbiddenException;
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

    private static final String USER_HEADER = "X-User-Id";
    private static final String VERIFIED_HEADER = "X-User-Verified";

    private final TicketService ticketService;

    @Operation(summary = "Create a new ticket", description = "Creates a ticket for the authenticated user; any userId in the body is ignored")
    @ApiResponse(responseCode = "200", description = "Ticket successfully created")
    @PostMapping("/add")
    public ResponseEntity<TicketResponse> add(@RequestHeader(USER_HEADER) Long callerId,
                                              @RequestHeader(value = VERIFIED_HEADER, defaultValue = "false") boolean verified,
                                              @Valid @RequestBody TicketRequest request) {
        if (!verified) {
            throw new ForbiddenException("Verify your email before adding tickets.");
        }
        return ResponseEntity.ok(ticketService.add(callerId, request));
    }

    @Operation(summary = "Get ticket by ID", description = "Retrieves one of the caller's tickets by its ID")
    @ApiResponse(responseCode = "200", description = "Ticket retrieved successfully")
    @GetMapping("/{id}")
    public ResponseEntity<TicketResponse> getById(@RequestHeader(USER_HEADER) Long callerId,
                                                  @PathVariable Long id) {
        return ResponseEntity.ok(ticketService.getById(callerId, id));
    }

    @Operation(summary = "Get tickets by user ID", description = "Retrieves the caller's tickets; only the caller's own id is accepted")
    @ApiResponse(responseCode = "200", description = "Tickets retrieved successfully")
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<TicketResponse>> getByUserId(@RequestHeader(USER_HEADER) Long callerId,
                                                            @PathVariable Long userId) {
        return ResponseEntity.ok(ticketService.getByUserId(callerId, userId));
    }

    @Operation(summary = "Get tickets by event ID", description = "Retrieves the caller's own tickets for a specific event")
    @ApiResponse(responseCode = "200", description = "Tickets retrieved successfully")
    @GetMapping("/event/{eventId}")
    public ResponseEntity<List<TicketResponse>> getByEventId(@RequestHeader(USER_HEADER) Long callerId,
                                                             @PathVariable Long eventId) {
        return ResponseEntity.ok(ticketService.getByEventId(callerId, eventId));
    }

    @Operation(summary = "Get all tickets", description = "Returns all tickets owned by the caller")
    @ApiResponse(responseCode = "200", description = "Tickets retrieved successfully")
    @GetMapping
    public ResponseEntity<List<TicketResponse>> getAll(@RequestHeader(USER_HEADER) Long callerId) {
        return ResponseEntity.ok(ticketService.getAll(callerId));
    }

    @Operation(summary = "Delete a ticket", description = "Deletes one of the caller's tickets by its ID")
    @ApiResponse(responseCode = "204", description = "Ticket successfully deleted")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@RequestHeader(USER_HEADER) Long callerId,
                                       @PathVariable Long id) {
        ticketService.delete(callerId, id);
        return ResponseEntity.noContent().build();
    }
}
