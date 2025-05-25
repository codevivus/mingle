package com.mingle.api_gateway.service.impl;

import com.mingle.api_gateway.service.TokenBlacklistService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class TokenBlacklistServiceImpl implements TokenBlacklistService {

    private final ReactiveStringRedisTemplate redisTemplate;

    private String hashToken(String token) {
        return DigestUtils.sha256Hex(token);
    }

    @Override
    public Mono<Boolean> isTokenBlacklisted(String token) {
        String hashed = hashToken(token);
        return redisTemplate.hasKey("blacklist:" + hashed);
    }

    @Override
    public Mono<Void> blacklistToken(String token, long expirationSeconds) {
        String hashed = hashToken(token);
        return redisTemplate.opsForValue()
                .set("blacklist:" + hashed, "true", Duration.ofSeconds(expirationSeconds))
                .then();
    }
}
