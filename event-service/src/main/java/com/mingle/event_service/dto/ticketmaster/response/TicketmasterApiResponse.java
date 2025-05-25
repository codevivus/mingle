package com.mingle.event_service.dto.ticketmaster.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class TicketmasterApiResponse {

    @JsonProperty("_embedded")
    private TicketmasterEmbeddedResponse embedded;

    @JsonProperty("page")
    private TicketmasterPageDto page;
}
