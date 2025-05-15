package com.mingle.api_gateway.controller;

import com.mingle.api_gateway.service.TokenBlacklistService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/internal/token")
@RequiredArgsConstructor
public class TokenBlacklistController {

    private final TokenBlacklistService tokenBlacklistService;

    @PostMapping("/blacklist")
    public Mono<Void> blacklistToken(@RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader,
                                     @RequestParam("exp") long expInSeconds) {
        String token = authHeader.substring(7);
        return tokenBlacklistService.blacklistToken(token, expInSeconds);
    }
}
