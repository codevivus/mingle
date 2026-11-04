package com.mingle.a2srv_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/** Only the SHA-256 of the emailed token is stored, so a database leak does not leak usable links. */
@Entity
@Table(name = "email_verification_tokens", indexes = @Index(columnList = "token_hash", unique = true))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailVerificationToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "token_hash", nullable = false, length = 64)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "used_at")
    private Instant usedAt;
}
