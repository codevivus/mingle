package com.mingle.api_gateway.service;

import com.mingle.api_gateway.entity.ValidateResponse;
import reactor.core.publisher.Mono;

public interface TokenValidatorService {
    Mono<ValidateResponse> validateTokenReactive(String token);
}
