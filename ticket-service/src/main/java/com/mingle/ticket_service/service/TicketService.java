package com.mingle.ticket_service.service;

import com.mingle.ticket_service.dto.TicketRequest;
import com.mingle.ticket_service.dto.TicketResponse;

import java.util.List;

/**
 * Every operation is scoped to {@code callerId}, the user id the gateway puts in {@code X-User-Id}.
 */
public interface TicketService {
    TicketResponse add(Long callerId, TicketRequest ticketRequest);
    TicketResponse getById(Long callerId, Long id);
    List<TicketResponse> getByUserId(Long callerId, Long userId);
    List<TicketResponse> getByEventId(Long callerId, Long eventId);
    List<TicketResponse> getAll(Long callerId);
    void delete(Long callerId, Long id);
}
