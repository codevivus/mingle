package com.mingle.event_service.dto.ticketmaster.response;

import com.mingle.event_service.dto.ticketmaster.TicketmasterEventDto;
import com.mingle.event_service.dto.ticketmaster.TicketmasterVenueDto;
import lombok.Data;

import java.util.List;

@Data
public class TicketmasterEmbeddedResponse {
    private List<TicketmasterEventDto> events;
    private List<TicketmasterVenueDto> venues;
}

