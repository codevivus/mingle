package com.mingle.event_service.mapper;

import com.mingle.event_service.dto.ticketmaster.TicketmasterEventDto;
import com.mingle.event_service.model.Event;
import org.mapstruct.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Mapper(componentModel = "spring")
public interface TicketmasterEventMapper {

    @Mapping(target = "externalId", source = "id")
    @Mapping(target = "title", source = "name")
    @Mapping(target = "description", source = "info")
    @Mapping(target = "city", expression = "java(dto.get_embedded().getVenues().get(0).getCity().getName())")
    @Mapping(target = "venue", expression = "java(dto.get_embedded().getVenues().get(0).getName())")
    @Mapping(target = "startTime", expression = "java(parseDateTime(dto.getDates().getStart().getLocalDate(), dto.getDates().getStart().getLocalTime()))")
    @Mapping(target = "endTime", ignore = true)
    @Mapping(target = "imageUrl", expression = "java(dto.getImages() != null && !dto.getImages().isEmpty() ? dto.getImages().get(0).getUrl() : null)")
    @Mapping(target = "imported", constant = "true")
    Event toEntity(TicketmasterEventDto dto);

    default LocalDateTime parseDateTime(String date, String time) {
        return LocalDateTime.parse(date + "T" + time, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }
}
