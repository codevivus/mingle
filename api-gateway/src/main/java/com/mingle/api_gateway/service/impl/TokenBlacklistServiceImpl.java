package com.mingle.api_gateway.service.impl;

import com.mingle.api_gateway.service.TokenBlacklistService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class TokenBlacklistServiceImpl implements TokenBlacklistService {

    private final ReactiveStringRedisTemplate redisTemplate;

    @Override
    public Mono<Boolean> isTokenBlacklisted(String token) {
        return redisTemplate.hasKey("blacklist:" + token);
    }

    @Override
    public Mono<Void> blacklistToken(String token, long expirationSeconds) {
        return redisTemplate.opsForValue()
                .set("blacklist:" + token, "true", java.time.Duration.ofSeconds(expirationSeconds))
                .then();
    }
}
