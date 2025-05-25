package com.mingle.event_service.dto.ticketmaster.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.mingle.event_service.dto.ticketmaster.TicketmasterEmbeddedDto;
import com.mingle.event_service.dto.ticketmaster.TicketmasterEventDto;

import lombok.Data;

import java.util.List;
import java.util.stream.Collectors;

@Data
public class TicketmasterVenueResponseWrapper {

    @JsonProperty("_embedded")
    private TicketmasterEmbeddedDto embedded;

    @JsonProperty("page")
    private TicketmasterPageDto page;

    public List<TicketmasterEventDto> extractEvents() {
        if (embedded == null || embedded.getVenues() == null) {
            return List.of();
        }

        return embedded.getVenues().stream()
                .map(venue -> {
                    TicketmasterEmbeddedDto embeddedDto = new TicketmasterEmbeddedDto();
                    embeddedDto.setVenues(List.of(venue));
                    TicketmasterEventDto dto = new TicketmasterEventDto();
                    dto.set_embedded(embeddedDto);
                    return dto;
                })
                .collect(Collectors.toList());
    }

}
