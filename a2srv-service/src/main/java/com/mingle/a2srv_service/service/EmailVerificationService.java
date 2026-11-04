package com.mingle.a2srv_service.service;

import com.mingle.a2srv_service.entity.EmailVerificationToken;
import com.mingle.a2srv_service.entity.User;
import com.mingle.a2srv_service.mail.MailService;
import com.mingle.a2srv_service.repository.EmailVerificationTokenRepository;
import com.mingle.a2srv_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final EmailVerificationTokenRepository tokens;
    private final UserRepository users;
    private final MailService mailService;

    @Value("${app.public-url:http://localhost:8080}")
    private String publicUrl;

    @Value("${verification.ttl-hours:24}")
    private long ttlHours;

    /**
     * Replaces any earlier link with a new one and mails it. A mail failure is logged, never thrown:
     * registration must not fail because SMTP is down, and the user can ask for another mail.
     */
    @Transactional
    public void sendFor(User user) {
        tokens.deleteByUserId(user.getId());
        byte[] raw = new byte[32];
        RANDOM.nextBytes(raw);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
        tokens.save(EmailVerificationToken.builder()
                .userId(user.getId())
                .tokenHash(sha256(token))
                .expiresAt(Instant.now().plus(Duration.ofHours(ttlHours)))
                .build());
        try {
            mailService.sendVerification(user.getEmail(), publicUrl + "/auth/verify?token=" + token);
        } catch (RuntimeException e) {
            log.warn("Could not send the verification mail to user {}: {}", user.getId(), e.getMessage());
        }
    }

    @Transactional
    public void verify(String token) {
        EmailVerificationToken stored = tokens.findByTokenHash(sha256(token == null ? "" : token))
                .filter(t -> t.getUsedAt() == null && t.getExpiresAt().isAfter(Instant.now()))
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired verification link"));
        User user = users.findById(stored.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired verification link"));
        user.setVerified(true);
        users.save(user);
        stored.setUsedAt(Instant.now());
        tokens.save(stored);
    }

    private static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Silent when the address is unknown or already verified, so it cannot be used to probe accounts. */
    @Transactional
    public void resend(String email) {
        if (email == null) {
            return;
        }
        users.findByEmail(email.trim())
                .filter(u -> !Boolean.TRUE.equals(u.getVerified()))
                .ifPresent(this::sendFor);
    }
}
