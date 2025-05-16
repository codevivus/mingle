package com.mingle.event_service.repository;

import com.mingle.event_service.model.Event;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface EventRepository extends JpaRepository<Event, Long> {
    Optional<Event> findByExternalId(String externalId);
    int deleteByImportedTrueAndCityAndEndTimeBefore(String city, LocalDateTime before);
}
