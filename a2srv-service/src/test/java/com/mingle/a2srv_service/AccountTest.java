package com.mingle.a2srv_service;

import com.mingle.a2srv_service.client.InternalServiceClient;
import com.mingle.a2srv_service.entity.User;
import com.mingle.a2srv_service.exception.type.DependencyUnavailableException;
import com.mingle.a2srv_service.repository.EmailVerificationTokenRepository;
import com.mingle.a2srv_service.repository.UserRepository;
import com.mingle.a2srv_service.security.JwtTokenProvider;
import com.mingle.a2srv_service.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(PostgresTestConfig.class)
class AccountTest {

    private static final String PASSWORD = "Passw0rd!123";

    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired EmailVerificationTokenRepository verificationTokens;
    @Autowired JwtTokenProvider jwt;
    @Autowired PasswordEncoder passwordEncoder;
    @MockBean InternalServiceClient internalServices;
    @MockBean AuthService authService;

    @BeforeEach
    void resetMocks() {
        reset(internalServices, authService);
    }

    private User newUser() {
        String name = "acc" + UUID.randomUUID().toString().substring(0, 8);
        User user = users.save(User.builder().username(name).email(name + "@example.com")
                .passwordHash(passwordEncoder.encode(PASSWORD)).verified(true)
                .interests(List.of("music", "hiking")).build());
        return user;
    }

    private String bearer(User user) {
        return "Bearer " + jwt.generateToken(String.valueOf(user.getId()));
    }

    private String body(String password) {
        return "{\"password\":\"" + password + "\"}";
    }

    @Test
    void deletingRemovesOtherServicesDataFirstThenTheAccount() throws Exception {
        User user = newUser();

        mvc.perform(delete("/user/me").header("Authorization", bearer(user))
                        .contentType(MediaType.APPLICATION_JSON).content(body(PASSWORD)))
                .andExpect(status().isNoContent());

        var order = inOrder(internalServices);
        order.verify(internalServices).purgeMatchData(user.getId());
        order.verify(internalServices).purgeTicketData(user.getId());
        assertThat(users.findById(user.getId())).isEmpty();
        verify(authService).logout(startsWith("Bearer "));
    }

    @Test
    void aWrongPasswordDeletesNothing() throws Exception {
        User user = newUser();

        mvc.perform(delete("/user/me").header("Authorization", bearer(user))
                        .contentType(MediaType.APPLICATION_JSON).content(body("not-the-password")))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(internalServices);
        assertThat(users.findById(user.getId())).isPresent();
    }

    @Test
    void aFailingServiceLeavesTheAccountIntactAndTheCallRepeatable() throws Exception {
        User user = newUser();
        doThrow(new DependencyUnavailableException("ticket-service did not answer", new RuntimeException("down")))
                .when(internalServices).purgeTicketData(anyLong());

        mvc.perform(delete("/user/me").header("Authorization", bearer(user))
                        .contentType(MediaType.APPLICATION_JSON).content(body(PASSWORD)))
                .andExpect(status().isBadGateway());
        assertThat(users.findById(user.getId())).isPresent();

        doNothing().when(internalServices).purgeTicketData(anyLong());
        mvc.perform(delete("/user/me").header("Authorization", bearer(user))
                        .contentType(MediaType.APPLICATION_JSON).content(body(PASSWORD)))
                .andExpect(status().isNoContent());
        assertThat(users.findById(user.getId())).isEmpty();
    }

    @Test
    void deletingNeedsAValidToken() throws Exception {
        mvc.perform(delete("/user/me").contentType(MediaType.APPLICATION_JSON).content(body(PASSWORD)))
                .andExpect(status().is4xxClientError());
        mvc.perform(delete("/user/me").header("Authorization", "Bearer garbage")
                        .contentType(MediaType.APPLICATION_JSON).content(body(PASSWORD)))
                .andExpect(status().isForbidden());
        verifyNoInteractions(internalServices);
    }

    @Test
    void exportCollectsTheProfileTicketsAndActivity() throws Exception {
        User user = newUser();
        when(internalServices.exportTickets(user.getId())).thenReturn(List.of(Map.of("id", 5, "eventId", 10)));
        when(internalServices.exportMatchData(user.getId())).thenReturn(Map.of("swipes", List.of(), "matches", List.of()));

        mvc.perform(get("/user/me/export").header("Authorization", bearer(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.account.id").value(user.getId()))
                .andExpect(jsonPath("$.account.email").value(user.getEmail()))
                .andExpect(jsonPath("$.profile.username").value(user.getUsername()))
                .andExpect(jsonPath("$.profile.interests.length()").value(2))
                .andExpect(jsonPath("$.tickets[0].eventId").value(10))
                .andExpect(jsonPath("$.activity.swipes").isArray())
                // The password hash is never part of an export.
                .andExpect(jsonPath("$.profile.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.account.passwordHash").doesNotExist());
    }

    @Test
    void exportFailsCleanlyWhenAServiceIsDown() throws Exception {
        User user = newUser();
        when(internalServices.exportTickets(anyLong()))
                .thenThrow(new DependencyUnavailableException("ticket-service did not answer", new RuntimeException("down")));

        mvc.perform(get("/user/me/export").header("Authorization", bearer(user))).andExpect(status().isBadGateway());
    }
}
