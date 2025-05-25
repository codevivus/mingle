package com.mingle.event_service.dto.ticketmaster.response;

import com.mingle.event_service.dto.ticketmaster.TicketmasterEventDto;
import com.mingle.event_service.dto.ticketmaster.TicketmasterVenueDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TicketmasterEmbeddedResponse {
    private List<TicketmasterEventDto> events;
    private List<TicketmasterVenueDto> venues;
}

