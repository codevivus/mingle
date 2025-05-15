package com.mingle.api_gateway.service;

import reactor.core.publisher.Mono;

public interface TokenValidatorService {
    Mono<String> validateTokenReactive(String token);
}
