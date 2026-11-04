package com.mingle.match_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "conversations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Conversation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "match_id", nullable = false, unique = true)
    private Long matchId;

    @Column(name = "event_id", nullable = false)
    private Long eventId;

    @Column(name = "user_one", nullable = false)
    private Long userOne;

    @Column(name = "user_two", nullable = false)
    private Long userTwo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ConversationStatus status = ConversationStatus.ACTIVE;

    /** From this instant the chat is read-only (no sending) unless it has been unlocked. */
    @Column(name = "lock_at", nullable = false)
    private Instant lockAt;

    /** From this instant the history is locked too, unless the chat has been unlocked. */
    @Column(name = "read_until", nullable = false)
    private Instant readUntil;

    @Column(name = "unlocked_at")
    private Instant unlockedAt;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    /** Set when the match ends (unmatch or block); a closed conversation cannot be read, written or unlocked. */
    @Column(name = "closed_at")
    private Instant closedAt;

    public boolean hasParticipant(Long userId) {
        return userOne.equals(userId) || userTwo.equals(userId);
    }
}
