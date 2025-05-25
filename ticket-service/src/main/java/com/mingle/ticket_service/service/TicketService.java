package com.mingle.ticket_service.service;

import com.mingle.ticket_service.dto.TicketRequest;
import com.mingle.ticket_service.dto.TicketResponse;

public interface TicketService {

    TicketResponse add(TicketRequest ticketRequest);
}
