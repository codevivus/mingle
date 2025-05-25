package com.mingle.a2srv_service.controller;

import com.mingle.a2srv_service.dto.AuthRequest;
import com.mingle.a2srv_service.dto.AuthResponse;
import com.mingle.a2srv_service.dto.RefreshTokenRequest;
import com.mingle.a2srv_service.dto.ValidateResponse;
import com.mingle.a2srv_service.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Handles authentication operations including login, token validation,
 * logout and access token renewal via refresh tokens.
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Endpoints for login, logout, token validation and refresh")
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "User login", description = "Authenticates user and returns JWT access & refresh tokens")
    @ApiResponse(responseCode = "200", description = "Successful login")
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody @Valid AuthRequest request) {
        return ResponseEntity.ok(authService.authenticate(request));
    }

    @Operation(summary = "Validate token", description = "Checks whether the provided access token is valid")
    @ApiResponse(responseCode = "200", description = "Token is valid")
    @PostMapping("/validate")
    public ResponseEntity<ValidateResponse> validateToken(@RequestHeader("Authorization") String bearerToken) {
        return ResponseEntity.ok(authService.validate(bearerToken));
    }

    @Operation(summary = "Logout", description = "Revokes the current token by adding it to the blacklist")
    @ApiResponse(responseCode = "200", description = "Token successfully blacklisted")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader("Authorization") String token) {
        authService.logout(token);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Refresh access token", description = "Issues a new access token using a valid refresh token")
    @ApiResponse(responseCode = "200", description = "New access token issued")
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refreshAccessToken(@RequestBody @Valid RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refreshAccessToken(request));
    }
}
