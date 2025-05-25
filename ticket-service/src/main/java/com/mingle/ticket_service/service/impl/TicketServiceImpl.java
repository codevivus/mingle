package com.mingle.ticket_service.service.impl;

import com.mingle.ticket_service.client.EventServiceClient;
import com.mingle.ticket_service.client.UserServiceClient;
import com.mingle.ticket_service.client.dto.request.EventIdsRequest;
import com.mingle.ticket_service.client.dto.request.UserIdsRequest;
import com.mingle.ticket_service.client.dto.response.EventResponse;
import com.mingle.ticket_service.client.dto.response.UserResponse;
import com.mingle.ticket_service.dto.TicketRequest;
import com.mingle.ticket_service.dto.TicketResponse;
import com.mingle.ticket_service.entity.Ticket;
import com.mingle.ticket_service.mapper.TicketMapper;
import com.mingle.ticket_service.repository.TicketRepository;
import com.mingle.ticket_service.service.TicketService;
import com.mingle.ticket_service.validation.TicketRequestValidator;
import jakarta.validation.ValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.stereotype.Service;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TicketServiceImpl implements TicketService {

    private final TicketMapper ticketMapper;
    private final TicketRepository ticketRepository;
    private final EventServiceClient eventServiceClient;
    private final UserServiceClient userServiceClient;
    private final TicketRequestValidator requestValidator;

    @Override
    public TicketResponse add(TicketRequest ticketRequest) {
        EventIdsRequest eventIdsRequest = EventIdsRequest.builder().eventId(ticketRequest.getEventId()).build();
        UserIdsRequest userIdsRequest = UserIdsRequest.builder().userId(ticketRequest.getEventId()).build();

        validate(ticketRequest);

        Ticket ticket = ticketRepository.save(ticketMapper.toEntity(ticketRequest));

        EventResponse eventResponse = eventServiceClient.findById(eventIdsRequest.getEventId()).getBody();
        UserResponse userResponse = userServiceClient.findById(userIdsRequest.getUserId()).getBody();

        return ticketMapper.toResponse(ticket, eventResponse, userResponse);
    }

    private void validate(TicketRequest request) {
        Errors errors = new BeanPropertyBindingResult(request, "ticketRequest");
        requestValidator.validate(request, errors);
        if (errors.hasErrors()) {
            List<String> messages = errors.getAllErrors().stream()
                    .map(DefaultMessageSourceResolvable::getDefaultMessage)
                    .toList();
            throw new ValidationException(messages.toString());
        }
    }
}
