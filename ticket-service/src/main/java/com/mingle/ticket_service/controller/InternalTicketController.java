package com.mingle.ticket_service.controller;

import com.mingle.ticket_service.repository.TicketRepository;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Service-to-service endpoints. The gateway has no route for /internal and InternalKeyFilter requires the
 * shared key, so only other services can call them.
 */
@Hidden
@RestController
@RequestMapping("/internal")
@RequiredArgsConstructor
public class InternalTicketController {

    private final TicketRepository ticketRepository;

    @GetMapping("/tickets/event/{eventId}/holders")
    public List<Long> holders(@PathVariable Long eventId) {
        return ticketRepository.findDistinctUserIdsByEventId(eventId);
    }

    /** Right of access: ticket metadata without the image bytes. */
    @GetMapping("/users/{userId}/tickets")
    public List<Map<String, Object>> tickets(@PathVariable Long userId) {
        return ticketRepository.findByUserId(userId).stream().map(t -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", t.getId());
            row.put("eventId", t.getEventId());
            row.put("status", t.getStatus());
            row.put("hasImage", t.getImageData() != null && t.getImageData().length > 0);
            return row;
        }).toList();
    }

    /** Right to erasure. Idempotent: deleting a user with no tickets succeeds. */
    @DeleteMapping("/users/{userId}")
    @Transactional
    public ResponseEntity<Map<String, Long>> purge(@PathVariable Long userId) {
        return ResponseEntity.ok(Map.of("tickets", ticketRepository.deleteByUserId(userId)));
    }
}
