package com.mingle.match_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

/** Ticket ownership lives in ticket-service; this is the only place match-service reads it. */
@FeignClient(name = "ticket-service")
public interface TicketClient {

    @GetMapping("/internal/tickets/event/{eventId}/holders")
    List<Long> holders(@PathVariable("eventId") Long eventId);
}
