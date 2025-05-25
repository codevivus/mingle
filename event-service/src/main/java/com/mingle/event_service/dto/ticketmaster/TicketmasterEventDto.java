package com.mingle.event_service.dto.ticketmaster;

import lombok.Data;
import java.util.List;

@Data
public class TicketmasterEventDto {
    private String id;
    private String name;
    private String info;
    private TicketmasterDatesDto dates;
    private List<TicketmasterImageDto> images;
    private List<TicketmasterVenueDto> venues;
}
