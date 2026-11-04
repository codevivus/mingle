package com.mingle.match_service;

import com.mingle.match_service.client.TicketClient;
import com.mingle.match_service.repository.MatchRepository;
import com.mingle.match_service.repository.SwipeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(PostgresTestConfig.class)
class MatchFlowTest {

    private static final String USER = "X-User-Id";

    @Autowired MockMvc mvc;
    @Autowired SwipeRepository swipes;
    @Autowired MatchRepository matches;
    @MockBean TicketClient ticketClient;
    @MockBean com.mingle.match_service.client.EventClient eventClient;
    @MockBean com.mingle.match_service.client.UserClient userClient;

    @BeforeEach
    void setUp() {
        matches.deleteAll();
        swipes.deleteAll();
        when(userClient.findById(2L)).thenReturn(new com.mingle.match_service.client.UserClient.Profile(
                2L, "bea", "Bea", "Kaya", 27, "Istanbul", "http://img/2.png", "hi"));
        when(userClient.findById(3L)).thenThrow(new RuntimeException("profile service down"));
        // A new match opens a conversation, which needs the event date.
        when(eventClient.findById(10L)).thenReturn(new com.mingle.match_service.client.EventClient.EventInfo(
                10L, java.time.LocalDateTime.now(), java.time.LocalDateTime.now().plusDays(1)));
        // Users 1, 2 and 3 hold tickets for event 10; user 4 only for event 11.
        when(ticketClient.holders(10L)).thenReturn(List.of(1L, 2L, 3L));
        when(ticketClient.holders(11L)).thenReturn(List.of(4L));
    }

    private ResultActions swipe(long caller, long eventId, long target, String action) throws Exception {
        return mvc.perform(post("/match/events/" + eventId + "/swipe")
                .header(USER, caller).header("X-User-Verified", "true")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"targetUserId\":" + target + ",\"action\":\"" + action + "\"}"));
    }

    @Test
    void candidatesAreOtherTicketHoldersOfThatEvent() throws Exception {
        mvc.perform(get("/match/events/10/candidates").header(USER, 1).header("X-User-Verified", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].userId").value(2))
                .andExpect(jsonPath("$[0].name").value("Bea"))
                .andExpect(jsonPath("$[0].photoUrl").value("http://img/2.png"))
                .andExpect(jsonPath("$[1].userId").value(3))
                .andExpect(jsonPath("$[1].name").doesNotExist());
    }

    @Test
    void candidatesExcludeLikedAndPassed() throws Exception {
        swipe(1, 10, 2, "LIKE").andExpect(status().isOk());
        swipe(1, 10, 3, "PASS").andExpect(status().isOk());

        mvc.perform(get("/match/events/10/candidates").header(USER, 1).header("X-User-Verified", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
        // Swipes by user 1 do not hide user 1 from user 2.
        mvc.perform(get("/match/events/10/candidates").header(USER, 2).header("X-User-Verified", "true"))
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void mutualLikeCreatesOneMatchForTheEvent() throws Exception {
        swipe(1, 10, 2, "LIKE")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matched").value(false));
        swipe(2, 10, 1, "LIKE")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matched").value(true));

        assertThat(matches.findAll()).hasSize(1);
        mvc.perform(get("/match/matches").header(USER, 1))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].eventId").value(10))
                .andExpect(jsonPath("$[0].otherUserId").value(2));
        mvc.perform(get("/match/matches").header(USER, 2))
                .andExpect(jsonPath("$[0].otherUserId").value(1));
        mvc.perform(get("/match/matches").param("eventId", "99").header(USER, 1))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void likeThenPassDoesNotMatch() throws Exception {
        swipe(1, 10, 2, "LIKE").andExpect(status().isOk());
        swipe(2, 10, 1, "PASS")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matched").value(false));

        assertThat(matches.findAll()).isEmpty();
    }

    @Test
    void repeatingASwipeIsIdempotent() throws Exception {
        swipe(1, 10, 2, "LIKE").andExpect(status().isOk());
        swipe(2, 10, 1, "LIKE").andExpect(jsonPath("$.matched").value(true));
        swipe(2, 10, 1, "LIKE").andExpect(jsonPath("$.matched").value(true));

        assertThat(matches.findAll()).hasSize(1);
        assertThat(swipes.findAll()).hasSize(2);
    }

    @Test
    void callerWithoutTicketIsForbidden() throws Exception {
        mvc.perform(get("/match/events/10/candidates").header(USER, 4).header("X-User-Verified", "true")).andExpect(status().isForbidden());
        swipe(4, 10, 1, "LIKE").andExpect(status().isForbidden());
    }

    @Test
    void targetWithoutTicketIsRejected() throws Exception {
        swipe(1, 10, 4, "LIKE").andExpect(status().isBadRequest());
        assertThat(swipes.findAll()).isEmpty();
    }

    @Test
    void cannotSwipeOnYourself() throws Exception {
        swipe(1, 10, 1, "LIKE").andExpect(status().isBadRequest());
    }

    @Test
    void missingHeaderIsUnauthorized() throws Exception {
        mvc.perform(get("/match/matches")).andExpect(status().isUnauthorized());
    }

    @Test
    void candidatesCanBeLimited() throws Exception {
        mvc.perform(get("/match/events/10/candidates").param("limit", "1").header(USER, 1).header("X-User-Verified", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].userId").value(2));
    }

    @Test
    void unverifiedUsersCannotListCandidatesOrSwipe() throws Exception {
        mvc.perform(get("/match/events/10/candidates").header(USER, 1)).andExpect(status().isForbidden());
        mvc.perform(post("/match/events/10/swipe").header(USER, 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetUserId\":2,\"action\":\"LIKE\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/match/events/10/candidates").header(USER, 1).header("X-User-Verified", "false"))
                .andExpect(status().isForbidden());
        assertThat(swipes.findAll()).isEmpty();
    }
}
