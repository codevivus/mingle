package com.mingle.a2srv_service.service.impl;

import com.mingle.a2srv_service.entity.Role;
import com.mingle.a2srv_service.dto.AuthRequest;
import com.mingle.a2srv_service.dto.AuthResponse;
import com.mingle.a2srv_service.dto.RefreshTokenRequest;
import com.mingle.a2srv_service.dto.ValidateResponse;
import com.mingle.a2srv_service.entity.User;
import com.mingle.a2srv_service.exception.type.InvalidCredentialsException;
import com.mingle.a2srv_service.exception.type.UserNotFoundException;
import com.mingle.a2srv_service.mapper.AuthMapper;
import com.mingle.a2srv_service.repository.UserRepository;
import com.mingle.a2srv_service.security.JwtTokenProvider;
import com.mingle.a2srv_service.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    @Value("${gateway.token-blacklist-url}")
    private String tokenBlacklistUrl;

    @Value("${gateway.internal-api-key}")
    private String internalApiKey;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthMapper authMapper;
    private final WebClient webClient;

    @Override
    public AuthResponse authenticate(AuthRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new InvalidCredentialsException("Email not found"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new InvalidCredentialsException("Invalid password");
        }

        return createAuthResponse(user);
    }

    @Override
    public ValidateResponse validate(String bearerToken) {
        String token = jwtTokenProvider.resolveToken(bearerToken);
        jwtTokenProvider.validateToken(token);
        String userId = jwtTokenProvider.getUserIdFromToken(token);

        ValidateResponse response = new ValidateResponse();
        response.setValid(true);
        response.setUserId(userId);
        // Read role and verification from the database on every validation so changes take effect immediately.
        User user = userRepository.findById(Long.parseLong(userId)).orElse(null);
        response.setRole(user != null && user.getRole() != null ? user.getRole().name() : Role.USER.name());
        response.setVerified(user != null && Boolean.TRUE.equals(user.getVerified()));
        return response;
    }

    @Override
    public void logout(String bearerToken) {
        String token = jwtTokenProvider.resolveToken(bearerToken);
        jwtTokenProvider.validateToken(token);
        // getRemainingValidity is in milliseconds; the gateway stores the entry for this many seconds.
        long exp = Math.max(1, (jwtTokenProvider.getRemainingValidity(token) + 999) / 1000);

        webClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path(tokenBlacklistUrl)
                        .queryParam("exp", exp)
                        .build())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .header("X-Internal-Key", internalApiKey)
                .retrieve()
                .bodyToMono(Void.class)
                .block();
    }

    @Override
    public AuthResponse refreshAccessToken(RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();
        jwtTokenProvider.validateToken(refreshToken);

        String userId = jwtTokenProvider.getUserIdFromToken(refreshToken);
        Long id = Long.parseLong(userId);

        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        String newAccessToken = jwtTokenProvider.generateToken(userId);
        return authMapper.toResponse(user, newAccessToken, refreshToken);
    }

    private AuthResponse createAuthResponse(User user) {
        String accessToken = jwtTokenProvider.generateToken(user.getId().toString());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId().toString());
        return authMapper.toResponse(user, accessToken, refreshToken);
    }
}
