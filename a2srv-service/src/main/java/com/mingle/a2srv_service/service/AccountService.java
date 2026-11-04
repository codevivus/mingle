package com.mingle.a2srv_service.service;

import com.mingle.a2srv_service.client.InternalServiceClient;
import com.mingle.a2srv_service.dto.UserResponse;
import com.mingle.a2srv_service.entity.User;
import com.mingle.a2srv_service.exception.type.InvalidCredentialsException;
import com.mingle.a2srv_service.exception.type.UserNotFoundException;
import com.mingle.a2srv_service.mapper.UserMapper;
import com.mingle.a2srv_service.repository.EmailVerificationTokenRepository;
import com.mingle.a2srv_service.repository.UserRepository;
import com.mingle.a2srv_service.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/** Right to erasure and right of access. */
@Slf4j
@Service
@RequiredArgsConstructor
public class AccountService {

    private final UserRepository users;
    private final EmailVerificationTokenRepository verificationTokens;
    private final InternalServiceClient internalServices;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final AuthService authService;

    /**
     * Removes the data other services hold first and the account last, so a failure leaves the account intact
     * and the call can simply be repeated (every purge is idempotent). The password is required so a stolen
     * token alone cannot erase an account.
     */
    @Transactional
    public void deleteAccount(String bearerToken, String password) {
        User user = currentUser(bearerToken);
        if (password == null || !passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new InvalidCredentialsException("Invalid password");
        }
        internalServices.purgeMatchData(user.getId());
        internalServices.purgeTicketData(user.getId());
        verificationTokens.deleteByUserId(user.getId());
        users.delete(user);
        try {
            authService.logout(bearerToken); // the token must stop working too
        } catch (RuntimeException e) {
            log.warn("Account {} deleted but its token could not be blacklisted: {}", user.getId(), e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public Map<String, Object> export(String bearerToken) {
        User user = currentUser(bearerToken);
        UserResponse profile = userMapper.toResponse(user);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("exportedAt", Instant.now().toString());
        out.put("account", Map.of("id", user.getId(), "email", user.getEmail(), "role", String.valueOf(user.getRole()),
                "verified", Boolean.TRUE.equals(user.getVerified())));
        out.put("profile", profile);
        out.put("tickets", internalServices.exportTickets(user.getId()));
        out.put("activity", internalServices.exportMatchData(user.getId()));
        return out;
    }

    private User currentUser(String bearerToken) {
        String token = jwtTokenProvider.resolveToken(bearerToken);
        jwtTokenProvider.validateToken(token);
        return users.findById(Long.valueOf(jwtTokenProvider.getUserIdFromToken(token)))
                .orElseThrow(() -> new UserNotFoundException("User not found"));
    }
}
