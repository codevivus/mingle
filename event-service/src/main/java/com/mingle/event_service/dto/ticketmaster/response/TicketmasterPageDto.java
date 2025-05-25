package com.mingle.event_service.dto.ticketmaster.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TicketmasterPageDto {
    private int size;
    private int totalElements;
    private int totalPages;
    private int number;
}
