package com.mingle.event_service.exception.type;

import lombok.Getter;
import java.time.Duration;

@Getter
public class RateLimitException extends RuntimeException {
    private final Duration retryDelay;

    public RateLimitException(String message, Duration retryDelay) {
        super(message);
        this.retryDelay = retryDelay;
    }
}
