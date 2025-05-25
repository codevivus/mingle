package com.mingle.event_service.repository;

import com.mingle.event_service.model.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;

public interface EventRepository extends JpaRepository<Event, Long> {
    int deleteByImportedTrueAndCityAndEndTimeBefore(String city, LocalDateTime before);
    List<Event> findAllByExternalIdIn(List<String> externalIds);

}
