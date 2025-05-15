
package com.mingle.api_gateway.exception.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mingle.api_gateway.exception.model.ErrorResponse;
import com.mingle.api_gateway.exception.type.InvalidTokenException;
import com.mingle.api_gateway.exception.type.TokenExpiredException;
import com.mingle.api_gateway.exception.type.UnauthorizedException;
import io.micrometer.common.lang.NonNullApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

@Slf4j
@Component
@Order(-2)
@NonNullApi
@RequiredArgsConstructor
public class GlobalErrorHandler implements ErrorWebExceptionHandler {

    private final ObjectMapper objectMapper;

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        HttpStatus status = resolveHttpStatus(ex);
        String message = ex.getMessage();

        log.error("[EXCEPTION] {} - {}", exchange.getRequest().getPath(), message);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(message)
                .path(exchange.getRequest().getPath().value())
                .timestamp(LocalDateTime.now())
                .build();

        byte[] responseBytes;
        try {
            responseBytes = objectMapper.writeValueAsBytes(errorResponse);
        } catch (Exception e) {
            responseBytes = "{\"error\": \"Serialization error\"}".getBytes(StandardCharsets.UTF_8);
        }

        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        return exchange.getResponse()
                .writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(responseBytes)));
    }

    private HttpStatus resolveHttpStatus(Throwable ex) {
        if (ex instanceof TokenExpiredException) {
            return HttpStatus.UNAUTHORIZED;
        } else if (ex instanceof InvalidTokenException) {
            return HttpStatus.UNAUTHORIZED;
        } else if (ex instanceof UnauthorizedException) {
            return HttpStatus.FORBIDDEN;
        } else {
            return HttpStatus.BAD_REQUEST;
        }
    }
}
