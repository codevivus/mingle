package com.mingle.api_gateway.controller;

import com.mingle.api_gateway.service.TokenBlacklistService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/internal/token")
@RequiredArgsConstructor
@Tag(name = "Token Blacklist", description = "Internal endpoints for managing blacklisted JWT tokens")
public class TokenBlacklistController {

    private final TokenBlacklistService tokenBlacklistService;

    @Operation(
            summary = "Blacklist a JWT token",
            description = "Adds the provided token to the blacklist until its expiration time"
    )
    @ApiResponse(responseCode = "200", description = "Token successfully blacklisted")
    @PostMapping("/blacklist")
    public Mono<Void> blacklistToken(
            @Parameter(description = "Bearer token to be blacklisted", required = true)
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader,

            @Parameter(description = "Token expiration time in seconds", required = true)
            @RequestParam("exp") long expInSeconds) {

        String token = authHeader.substring(7);
        return tokenBlacklistService.blacklistToken(token, expInSeconds);
    }
}
