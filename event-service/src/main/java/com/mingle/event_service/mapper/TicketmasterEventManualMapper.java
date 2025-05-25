package com.mingle.event_service.mapper;

import com.mingle.event_service.dto.ticketmaster.TicketmasterEventDto;
import com.mingle.event_service.dto.ticketmaster.TicketmasterVenueDto;
import com.mingle.event_service.entity.Event;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class TicketmasterEventManualMapper {

    public Event toEntity(TicketmasterEventDto dto) {
        if (dto == null) return null;

        Event event = new Event();
        event.setExternalId(dto.getId());
        event.setTitle(dto.getName());
        event.setDescription(dto.getInfo());

        List<TicketmasterVenueDto> venues = dto.getVenues();
        if (venues != null && !venues.isEmpty()) {
            TicketmasterVenueDto venue = venues.get(0);

            event.setVenue(venue.getName());

            if (venue.getCity() != null && venue.getCity().getName() != null) {
                event.setCity(venue.getCity().getName());
            } else {
                event.setCity(null);
            }
        } else {
            event.setVenue(null);
            event.setCity(null);
        }

        if (dto.getDates() != null && dto.getDates().getStart() != null) {
            String date = dto.getDates().getStart().getLocalDate();
            String time = dto.getDates().getStart().getLocalTime();
            try {
                event.setStartTime(parseDateTime(date, time));
            } catch (Exception e) {
                event.setStartTime(null);
            }
        } else {
            event.setStartTime(null);
        }

        if (dto.getImages() != null && !dto.getImages().isEmpty()) {
            event.setImageUrl(dto.getImages().get(0).getUrl());
        } else {
            event.setImageUrl(null);
        }

        event.setImported(true);
        event.setEndTime(null);

        return event;
    }

    private LocalDateTime parseDateTime(String date, String time) {
        return LocalDateTime.parse(date + "T" + time, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }
}
