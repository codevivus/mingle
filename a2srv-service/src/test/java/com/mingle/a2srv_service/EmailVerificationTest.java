package com.mingle.a2srv_service;

import com.mingle.a2srv_service.entity.EmailVerificationToken;
import com.mingle.a2srv_service.mail.MailService;
import com.mingle.a2srv_service.repository.EmailVerificationTokenRepository;
import com.mingle.a2srv_service.repository.UserRepository;
import com.mingle.a2srv_service.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(PostgresTestConfig.class)
class EmailVerificationTest {

    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired EmailVerificationTokenRepository tokens;
    @Autowired JwtTokenProvider jwt;
    @MockBean MailService mailService;

    @BeforeEach
    void resetMail() {
        reset(mailService);
    }

    private String register(String name) throws Exception {
        mvc.perform(post("/user/register").contentType(MediaType.APPLICATION_JSON).content(
                        "{\"username\":\"" + name + "\",\"email\":\"" + name + "@example.com\",\"password\":\"Passw0rd!123\","
                                + "\"name\":\"" + name + "\",\"surname\":\"T\",\"age\":28,\"gender\":\"male\","
                                + "\"city\":\"Istanbul\",\"birthDate\":\"1997-01-01\"}"))
                .andExpect(status().is2xxSuccessful());
        return name + "@example.com";
    }

    private String lastTokenSentTo(String email) {
        ArgumentCaptor<String> link = ArgumentCaptor.forClass(String.class);
        verify(mailService, atLeastOnce()).sendVerification(eq(email), link.capture());
        String last = link.getAllValues().get(link.getAllValues().size() - 1);
        return last.substring(last.indexOf("token=") + 6);
    }

    private String unique() {
        return "v" + UUID.randomUUID().toString().substring(0, 8);
    }

    private void assertValidateVerified(String email, boolean expected) throws Exception {
        long id = users.findByEmail(email).orElseThrow().getId();
        mvc.perform(post("/auth/validate").header("Authorization", "Bearer " + jwt.generateToken(String.valueOf(id))))
                .andExpect(jsonPath("$.verified").value(expected));
    }

    @Test
    void registrationSendsALinkAndLeavesTheUserUnverified() throws Exception {
        String email = register(unique());

        String token = lastTokenSentTo(email);
        assertThat(token).hasSizeGreaterThan(30);
        assertThat(users.findByEmail(email).orElseThrow().getVerified()).isFalse();
        // The raw token is not stored.
        assertThat(tokens.findAll()).noneMatch(t -> t.getTokenHash().equals(token));
        assertValidateVerified(email, false);
    }

    @Test
    void theLinkVerifiesTheUserOnceOnly() throws Exception {
        String email = register(unique());
        String token = lastTokenSentTo(email);

        mvc.perform(get("/auth/verify").param("token", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verified").value(true));

        assertThat(users.findByEmail(email).orElseThrow().getVerified()).isTrue();
        assertValidateVerified(email, true);
        mvc.perform(get("/auth/verify").param("token", token)).andExpect(status().isBadRequest());
    }

    @Test
    void anExpiredLinkIsRejected() throws Exception {
        String email = register(unique());
        String token = lastTokenSentTo(email);
        EmailVerificationToken stored = tokens.findAll().stream()
                .filter(t -> t.getUserId().equals(users.findByEmail(email).orElseThrow().getId())).findFirst().orElseThrow();
        stored.setExpiresAt(Instant.now().minusSeconds(5));
        tokens.save(stored);

        mvc.perform(get("/auth/verify").param("token", token)).andExpect(status().isBadRequest());
        assertThat(users.findByEmail(email).orElseThrow().getVerified()).isFalse();
    }

    @Test
    void aMadeUpLinkIsRejected() throws Exception {
        mvc.perform(get("/auth/verify").param("token", "not-a-real-token")).andExpect(status().isBadRequest());
    }

    @Test
    void resendIssuesANewLinkAndInvalidatesTheOldOne() throws Exception {
        String email = register(unique());
        String first = lastTokenSentTo(email);
        reset(mailService);

        mvc.perform(post("/auth/resend-verification").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\"}"))
                .andExpect(status().isAccepted());

        String second = lastTokenSentTo(email);
        assertThat(second).isNotEqualTo(first);
        mvc.perform(get("/auth/verify").param("token", first)).andExpect(status().isBadRequest());
        mvc.perform(get("/auth/verify").param("token", second)).andExpect(status().isOk());
    }

    @Test
    void resendIsSilentForUnknownOrAlreadyVerifiedAddresses() throws Exception {
        mvc.perform(post("/auth/resend-verification").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"nobody@example.com\"}"))
                .andExpect(status().isAccepted());

        String email = register(unique());
        mvc.perform(get("/auth/verify").param("token", lastTokenSentTo(email))).andExpect(status().isOk());
        reset(mailService);
        mvc.perform(post("/auth/resend-verification").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\"}"))
                .andExpect(status().isAccepted());

        verify(mailService, never()).sendVerification(anyString(), anyString());
    }

    @Test
    void registrationSucceedsEvenWhenTheMailCannotBeSent() throws Exception {
        doThrow(new RuntimeException("smtp down")).when(mailService).sendVerification(anyString(), anyString());

        String email = register(unique());

        assertThat(users.findByEmail(email)).isPresent();
    }
}
