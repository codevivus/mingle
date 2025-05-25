package com.mingle.event_service.dto.ticketmaster;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TicketmasterVenueDto {
    private String name;
    private TicketmasterCityDto city;
}

