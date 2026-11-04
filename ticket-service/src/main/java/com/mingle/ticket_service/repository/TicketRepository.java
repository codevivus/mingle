package com.mingle.ticket_service.repository;

import com.mingle.ticket_service.entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TicketRepository extends JpaRepository<Ticket, Long> {
    List<Ticket> findByUserId(Long userId);

    long deleteByUserId(Long userId);
    List<Ticket> findByEventId(Long eventId);
    List<Ticket> findByUserIdAndEventId(Long userId, Long eventId);

    @Query("select distinct t.userId from Ticket t where t.eventId = :eventId")
    List<Long> findDistinctUserIdsByEventId(@Param("eventId") Long eventId);
}
