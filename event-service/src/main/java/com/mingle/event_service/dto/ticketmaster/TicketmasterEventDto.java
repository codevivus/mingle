package com.mingle.event_service.dto.ticketmaster;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TicketmasterEventDto {
    private String id;
    private String name;
    private String info;
    private TicketmasterDatesDto dates;
    private List<TicketmasterImageDto> images;
    private List<TicketmasterVenueDto> venues;

    /** The Discovery API nests an event's venues under its own "_embedded" object. */
    @JsonProperty("_embedded")
    private Embedded embedded;

    /** Venues from "_embedded" when present, otherwise the flat list. */
    public List<TicketmasterVenueDto> getVenues() {
        if (embedded != null && embedded.getVenues() != null && !embedded.getVenues().isEmpty()) {
            return embedded.getVenues();
        }
        return venues;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Embedded {
        private List<TicketmasterVenueDto> venues;
    }
}
