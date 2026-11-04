package com.mingle.ticket_service.controller;

import com.mingle.ticket_service.repository.TicketRepository;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Service-to-service lookups. The gateway has no route for /internal, so this is only reachable
 * from inside the network (match-service calls it through Eureka/Feign).
 */
@Hidden
@RestController
@RequestMapping("/internal/tickets")
@RequiredArgsConstructor
public class InternalTicketController {

    private final TicketRepository ticketRepository;

    @GetMapping("/event/{eventId}/holders")
    public List<Long> holders(@PathVariable Long eventId) {
        return ticketRepository.findDistinctUserIdsByEventId(eventId);
    }
}
