package com.mingle.event_service.mapper;

import com.mingle.event_service.dto.ticketmaster.TicketmasterEventDto;
import com.mingle.event_service.model.Event;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class TicketmasterEventManualMapper {

    public Event toEntity(TicketmasterEventDto dto) {
        if (dto == null) return null;

        Event event = new Event();

        event.setExternalId(dto.getId());
        event.setTitle(dto.getName());
        event.setDescription(dto.getInfo());

        try {
            String city = dto.get_embedded()
                    .getVenues()
                    .get(0)
                    .getCity()
                    .getName();
            event.setCity(city);
        } catch (Exception e) {
            event.setCity(null);
        }

        try {
            String venue = dto.get_embedded()
                    .getVenues()
                    .get(0)
                    .getName();
            event.setVenue(venue);
        } catch (Exception e) {
            event.setVenue(null);
        }

        try {
            String date = dto.getDates().getStart().getLocalDate();
            String time = dto.getDates().getStart().getLocalTime();
            event.setStartTime(parseDateTime(date, time));
        } catch (Exception e) {
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
