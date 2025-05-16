package com.mingle.event_service.dto.ticketmaster.response;

import lombok.Data;

@Data
public class TicketmasterPageDto {
    private int size;
    private int totalElements;
    private int totalPages;
    private int number;
}
