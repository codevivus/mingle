package com.mingle.event_service.repository;

import com.mingle.event_service.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

public interface EventRepository extends JpaRepository<Event, Long> {

    /**
     * Imported events carry no end time (Ticketmaster rarely sends one), so the start time decides.
     * Events created by hand are never removed.
     */
    @Transactional
    @Modifying
    @Query("delete from Event e where e.imported = true and coalesce(e.endTime, e.startTime) < :before")
    int deleteExpiredImported(@Param("before") LocalDateTime before);

    List<Event> findAllByExternalIdIn(List<String> externalIds);

}
