package com.mingle.event_service.dto.ticketmaster;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TicketmasterStartDto {
    private String localDate;
    private String localTime;
}
