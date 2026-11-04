package com.mingle.match_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "match_swipes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Swipe {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false)
    private Long eventId;

    @Column(name = "from_user", nullable = false)
    private Long fromUser;

    @Column(name = "to_user", nullable = false)
    private Long toUser;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SwipeAction action;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}
