package com.mingle.event_service.mapper;

import com.mingle.event_service.dto.EventRequestDto;
import com.mingle.event_service.dto.EventResponseDto;
import com.mingle.event_service.entity.Event;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring")
public interface EventMapper {

    Event toEntity(EventRequestDto dto);

    EventResponseDto toDto(Event entity);

    List<EventResponseDto> toDtoList(List<Event> entities);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEventFromDto(EventRequestDto dto, @MappingTarget Event entity);
}
