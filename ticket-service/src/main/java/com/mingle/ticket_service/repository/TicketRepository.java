package com.mingle.ticket_service.repository;

import com.mingle.ticket_service.entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TicketRepository extends JpaRepository<Ticket, Long> {
    List<Ticket> findByUserId(Long userId);
    List<Ticket> findByEventId(Long eventId);
    List<Ticket> findByUserIdAndEventId(Long userId, Long eventId);
}
