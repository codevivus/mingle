package com.mingle.ticket_service.mapper;

import com.mingle.ticket_service.client.dto.response.EventResponse;
import com.mingle.ticket_service.client.dto.response.UserResponse;
import com.mingle.ticket_service.dto.TicketRequest;
import com.mingle.ticket_service.dto.TicketResponse;
import com.mingle.ticket_service.entity.Ticket;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

@Mapper(componentModel = "spring")
public interface TicketMapper {

    Ticket toEntity(TicketRequest request);

    @Mappings({
            @Mapping(target = "id", source = "ticket.id"),
            @Mapping(target = "eventResponse", source = "eventResponse"),
            @Mapping(target = "userResponse", source = "userResponse"),
            @Mapping(target = "status", source = "ticket.status"),
            @Mapping(target = "imageData", source = "ticket.imageData")
    })
    TicketResponse toResponse(Ticket ticket, EventResponse eventResponse, UserResponse userResponse);

}
