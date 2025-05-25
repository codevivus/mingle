package com.mingle.ticket_service.service;

import com.mingle.ticket_service.dto.TicketRequest;
import com.mingle.ticket_service.dto.TicketResponse;

import java.util.List;

public interface TicketService {
    TicketResponse add(TicketRequest ticketRequest);
    TicketResponse getById(Long id);
    List<TicketResponse> getByUserId(Long userId);
    List<TicketResponse> getByEventId(Long eventId);
    List<TicketResponse> getAll();
    void delete(Long id);
}
