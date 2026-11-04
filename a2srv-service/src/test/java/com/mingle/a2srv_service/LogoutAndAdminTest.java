package com.mingle.a2srv_service;

import com.mingle.a2srv_service.config.AdminBootstrap;
import com.mingle.a2srv_service.entity.Role;
import com.mingle.a2srv_service.entity.User;
import com.mingle.a2srv_service.mapper.AuthMapper;
import com.mingle.a2srv_service.repository.UserRepository;
import com.mingle.a2srv_service.security.JwtTokenProvider;
import com.mingle.a2srv_service.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@SpringBootTest
@ActiveProfiles("test")
@Import(PostgresTestConfig.class)
class LogoutAndAdminTest {

    @Autowired UserRepository users;
    @Autowired JwtTokenProvider tokens;
    @Autowired AdminBootstrap adminBootstrap;

    @Test
    void logoutSendsTheBlacklistLifetimeInSeconds() {
        AtomicReference<URI> called = new AtomicReference<>();
        AtomicReference<String> key = new AtomicReference<>();
        WebClient client = WebClient.builder().exchangeFunction(request -> {
            called.set(request.url());
            key.set(request.headers().getFirst("X-Internal-Key"));
            return Mono.just(ClientResponse.create(org.springframework.http.HttpStatus.OK).build());
        }).build();
        AuthServiceImpl service = new AuthServiceImpl(
                mock(UserRepository.class), mock(PasswordEncoder.class), tokens, mock(AuthMapper.class), client);
        ReflectionTestUtils.setField(service, "tokenBlacklistUrl", "http://gateway/internal/token/blacklist");
        ReflectionTestUtils.setField(service, "internalApiKey", "k");

        service.logout("Bearer " + tokens.generateToken("1")); // access tokens live 3,600,000 ms

        String query = called.get().getQuery();
        long exp = Long.parseLong(query.replace("exp=", ""));
        assertThat(exp).isBetween(3590L, 3600L);
        assertThat(key.get()).isEqualTo("k");
    }

    @Test
    void adminEmailPromotesAnExistingAccountOnlyAtStartup() {
        users.save(User.builder().username("boss").email("boss@example.com").passwordHash("x").verified(true).build());
        users.save(User.builder().username("other").email("other@example.com").passwordHash("x").verified(true).build());
        ReflectionTestUtils.setField(adminBootstrap, "adminEmail", "boss@example.com");

        adminBootstrap.run(new DefaultApplicationArguments());

        assertThat(users.findByEmail("boss@example.com").orElseThrow().getRole()).isEqualTo(Role.ADMIN);
        assertThat(users.findByEmail("other@example.com").orElseThrow().getRole()).isEqualTo(Role.USER);
    }

    @Test
    void adminEmailDoesNotPromoteAnUnverifiedAccount() {
        users.save(User.builder().username("squatter").email("squatter@example.com").passwordHash("x").verified(false).build());
        ReflectionTestUtils.setField(adminBootstrap, "adminEmail", "squatter@example.com");

        adminBootstrap.run(new DefaultApplicationArguments());

        assertThat(users.findByEmail("squatter@example.com").orElseThrow().getRole()).isEqualTo(Role.USER);
    }

    @Test
    void adminEmailWithoutAMatchingAccountDoesNothing() {
        ReflectionTestUtils.setField(adminBootstrap, "adminEmail", "nobody@example.com");

        adminBootstrap.run(new DefaultApplicationArguments());

        assertThat(users.findByEmail("nobody@example.com")).isEmpty();
    }
}
