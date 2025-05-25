package com.mingle.ticket_service.dto;

import lombok.Data;

@Data
public class TicketRequest {

    private Long eventId;
    private Long userId;
    private byte[] imageData;

}
