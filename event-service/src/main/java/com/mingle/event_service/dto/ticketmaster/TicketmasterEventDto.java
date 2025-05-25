package com.mingle.event_service.dto.ticketmaster;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TicketmasterEventDto {
    private String id;
    private String name;
    private String info;
    private TicketmasterDatesDto dates;
    private List<TicketmasterImageDto> images;
    private List<TicketmasterVenueDto> venues;
}
