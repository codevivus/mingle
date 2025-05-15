package com.mingle.api_gateway.service;

import reactor.core.publisher.Mono;

public interface TokenBlacklistService {
    Mono<Boolean> isTokenBlacklisted(String token);
    Mono<Void> blacklistToken(String token, long expirationSeconds);
}
