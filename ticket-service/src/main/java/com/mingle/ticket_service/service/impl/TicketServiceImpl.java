package com.mingle.ticket_service.service.impl;

import com.mingle.ticket_service.client.EventServiceClient;
import com.mingle.ticket_service.client.UserServiceClient;
import com.mingle.ticket_service.client.dto.response.EventResponse;
import com.mingle.ticket_service.client.dto.response.UserResponse;
import com.mingle.ticket_service.dto.TicketRequest;
import com.mingle.ticket_service.dto.TicketResponse;
import com.mingle.ticket_service.entity.Ticket;
import com.mingle.ticket_service.exception.type.ExternalServiceException;
import com.mingle.ticket_service.exception.type.TicketNotFoundException;
import com.mingle.ticket_service.exception.type.ValidationException;
import com.mingle.ticket_service.mapper.TicketMapper;
import com.mingle.ticket_service.repository.TicketRepository;
import com.mingle.ticket_service.service.TicketService;
import com.mingle.ticket_service.validation.TicketRequestValidator;
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
        validate(ticketRequest);
        EventResponse eventResponse = eventServiceClient.findById(ticketRequest.getEventId()).getBody();
        UserResponse userResponse = userServiceClient.findById(ticketRequest.getUserId()).getBody();
        if (eventResponse == null) {
            throw new ExternalServiceException("Event service did not return a valid response.");
        }
        if (userResponse == null) {
            throw new ExternalServiceException("User service did not return a valid response.");
        }
        Ticket ticket = ticketRepository.save(ticketMapper.toEntity(ticketRequest));
        return ticketMapper.toResponse(ticket, eventResponse, userResponse);
    }

    @Override
    public TicketResponse getById(Long id) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new TicketNotFoundException("Ticket not found with ID: " + id));

        EventResponse event = eventServiceClient.findById(ticket.getEventId()).getBody();
        UserResponse user = userServiceClient.findById(ticket.getUserId()).getBody();

        if (event == null || user == null) {
            throw new ExternalServiceException("Related user or event data could not be retrieved.");
        }

        return ticketMapper.toResponse(ticket, event, user);
    }

    @Override
    public List<TicketResponse> getByUserId(Long userId) {
        return ticketRepository.findByUserId(userId).stream()
                .map(ticket -> {
                    EventResponse event = eventServiceClient.findById(ticket.getEventId()).getBody();
                    UserResponse user = userServiceClient.findById(ticket.getUserId()).getBody();
                    if (event == null || user == null) {
                        throw new ExternalServiceException("User or event not found for ticket ID: " + ticket.getId());
                    }
                    return ticketMapper.toResponse(ticket, event, user);
                })
                .toList();
    }

    @Override
    public List<TicketResponse> getByEventId(Long eventId) {
        return ticketRepository.findByEventId(eventId).stream()
                .map(ticket -> {
                    EventResponse event = eventServiceClient.findById(ticket.getEventId()).getBody();
                    UserResponse user = userServiceClient.findById(ticket.getUserId()).getBody();
                    if (event == null || user == null) {
                        throw new ExternalServiceException("User or event not found for ticket ID: " + ticket.getId());
                    }
                    return ticketMapper.toResponse(ticket, event, user);
                })
                .toList();
    }

    @Override
    public List<TicketResponse> getAll() {
        return ticketRepository.findAll().stream()
                .map(ticket -> {
                    EventResponse event = eventServiceClient.findById(ticket.getEventId()).getBody();
                    UserResponse user = userServiceClient.findById(ticket.getUserId()).getBody();
                    if (event == null || user == null) {
                        throw new ExternalServiceException("User or event not found for ticket ID: " + ticket.getId());
                    }
                    return ticketMapper.toResponse(ticket, event, user);
                })
                .toList();
    }

    @Override
    public void delete(Long id) {
        if (!ticketRepository.existsById(id)) {
            throw new TicketNotFoundException("Ticket not found with ID: " + id);
        }
        ticketRepository.deleteById(id);
    }

    private void validate(TicketRequest request) {
        Errors errors = new BeanPropertyBindingResult(request, "ticketRequest");
        requestValidator.validate(request, errors);
        if (errors.hasErrors()) {
            List<String> messages = errors.getAllErrors().stream()
                    .map(DefaultMessageSourceResolvable::getDefaultMessage)
                    .toList();
            throw new ValidationException(messages);
        }
    }
}
