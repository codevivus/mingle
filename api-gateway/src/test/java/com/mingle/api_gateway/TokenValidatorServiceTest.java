package com.mingle.api_gateway;

import com.mingle.api_gateway.service.impl.TokenValidatorServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** The gateway must call a2srv-service the way it is exposed: POST /auth/validate with the bearer token. */
class TokenValidatorServiceTest {

    private TokenValidatorServiceImpl service(AtomicReference<HttpMethod> method, AtomicReference<String> auth,
                                              HttpStatus status, String body) {
        WebClient client = WebClient.builder().exchangeFunction(request -> {
            method.set(request.method());
            auth.set(request.headers().getFirst(HttpHeaders.AUTHORIZATION));
            return Mono.just(ClientResponse.create(status)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .body(body).build());
        }).build();
        TokenValidatorServiceImpl service = new TokenValidatorServiceImpl(client);
        ReflectionTestUtils.setField(service, "validateUrl", "http://a2srv-service/auth/validate");
        return service;
    }

    @Test
    void validatesWithPostAndReturnsUserIdAndRole() {
        AtomicReference<HttpMethod> method = new AtomicReference<>();
        AtomicReference<String> auth = new AtomicReference<>();
        TokenValidatorServiceImpl service = service(method, auth, HttpStatus.OK, "{\"valid\":true,\"userId\":\"7\",\"role\":\"ADMIN\"}");

        com.mingle.api_gateway.entity.ValidateResponse r = service.validateTokenReactive("tok").block();
        assertThat(r.getUserId()).isEqualTo("7");
        assertThat(r.getRole()).isEqualTo("ADMIN");
        assertThat(method.get()).isEqualTo(HttpMethod.POST);
        assertThat(auth.get()).isEqualTo("Bearer tok");
    }

    @Test
    void rejectsAnInvalidToken() {
        TokenValidatorServiceImpl service = service(new AtomicReference<>(), new AtomicReference<>(), HttpStatus.UNAUTHORIZED, "{}");

        assertThatThrownBy(() -> service.validateTokenReactive("bad").block()).isNotNull();
    }
}
