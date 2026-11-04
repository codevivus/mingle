package com.mingle.match_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/** A mutual like for one event. {@code userOne} is always the smaller id, so a pair has one row. */
@Entity
@Table(name = "matches")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Match {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false)
    private Long eventId;

    @Column(name = "user_one", nullable = false)
    private Long userOne;

    @Column(name = "user_two", nullable = false)
    private Long userTwo;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "ended_at")
    private Instant endedAt;

    @Column(name = "ended_by")
    private Long endedBy;

    public boolean isActive() {
        return endedAt == null;
    }

    public boolean includes(Long userId) {
        return userOne.equals(userId) || userTwo.equals(userId);
    }

    public Long otherUser(Long userId) {
        return userOne.equals(userId) ? userTwo : userOne;
    }
}
