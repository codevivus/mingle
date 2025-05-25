package com.mingle.ticket_service.dto;

import com.mingle.ticket_service.client.dto.response.EventResponse;
import com.mingle.ticket_service.client.dto.response.UserResponse;
import com.mingle.ticket_service.enums.TicketStatus;
import lombok.*;

@Data
public class TicketResponse {

    private Long id;
    private EventResponse eventResponse;
    private UserResponse userResponse;
    private TicketStatus status;
    private byte[] imageData;

}
