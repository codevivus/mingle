package com.mingle.event_service.dto.ticketmaster;

import lombok.Data;
import java.util.List;

@Data
public class TicketmasterEmbeddedDto {
    private List<TicketmasterVenueDto> venues;
}
