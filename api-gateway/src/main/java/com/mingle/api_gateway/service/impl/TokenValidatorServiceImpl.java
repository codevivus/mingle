package com.mingle.api_gateway.service.impl;

import com.mingle.api_gateway.exception.type.InvalidTokenException;
import com.mingle.api_gateway.entity.ValidateResponse;
import com.mingle.api_gateway.service.TokenValidatorService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class TokenValidatorServiceImpl implements TokenValidatorService {

    private final WebClient webClient;

    @Value("${auth.service.validate-url}")
    private String validateUrl;

    @Override
    public Mono<ValidateResponse> validateTokenReactive(String token) {
        return webClient.get()
                .uri(validateUrl)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .bodyToMono(ValidateResponse.class)
                .flatMap(response -> {
                    if (response.isValid()) {
                        return Mono.just(response);
                    } else {
                        return Mono.error(new InvalidTokenException("Token validation failed"));
                    }
                })
                .onErrorMap(e -> new InvalidTokenException("Token validation failed"));
    }
}
