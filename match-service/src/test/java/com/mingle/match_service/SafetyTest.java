package com.mingle.match_service;

import com.mingle.match_service.client.EventClient;
import com.mingle.match_service.client.EventClient.EventInfo;
import com.mingle.match_service.client.TicketClient;
import com.mingle.match_service.client.UserClient;
import com.mingle.match_service.repository.BlockRepository;
import com.mingle.match_service.repository.ConversationRepository;
import com.mingle.match_service.repository.MatchRepository;
import com.mingle.match_service.repository.MessageRepository;
import com.mingle.match_service.repository.ReportRepository;
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

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(PostgresTestConfig.class)
class SafetyTest {

    private static final String USER = "X-User-Id";
    private static final String VERIFIED = "X-User-Verified";

    @Autowired MockMvc mvc;
    @Autowired MatchRepository matches;
    @Autowired SwipeRepository swipes;
    @Autowired ConversationRepository conversations;
    @Autowired MessageRepository messages;
    @Autowired BlockRepository blocks;
    @Autowired ReportRepository reports;
    @MockBean TicketClient ticketClient;
    @MockBean EventClient eventClient;
    @MockBean UserClient userClient;

    @BeforeEach
    void setUp() {
        messages.deleteAll();
        conversations.deleteAll();
        matches.deleteAll();
        swipes.deleteAll();
        blocks.deleteAll();
        reports.deleteAll();
        when(ticketClient.holders(10L)).thenReturn(List.of(1L, 2L, 3L));
        when(ticketClient.holders(11L)).thenReturn(List.of(1L, 2L));
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Europe/Istanbul"));
        for (long event : new long[]{10L, 11L}) {
            when(eventClient.findById(event)).thenReturn(new EventInfo(event, now, now.plusDays(1)));
        }
        when(userClient.findById(org.mockito.ArgumentMatchers.anyLong())).thenThrow(new RuntimeException("no profiles here"));
    }

    private ResultActions swipe(long caller, long event, long target) throws Exception {
        return mvc.perform(post("/match/events/" + event + "/swipe").header(USER, caller).header(VERIFIED, "true")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"targetUserId\":" + target + ",\"action\":\"LIKE\"}"));
    }

    /** Users 1 and 2 match on the given event; returns the match id. */
    private long matchOnEvent(long event) throws Exception {
        swipe(1, event, 2).andExpect(status().isOk());
        swipe(2, event, 1).andExpect(status().isOk());
        return matches.findAll().stream().filter(m -> m.getEventId() == event).findFirst().orElseThrow().getId();
    }

    private long conversationOf(long matchId) {
        return conversations.findByMatchId(matchId).orElseThrow().getId();
    }

    private ResultActions candidates(long caller, long event) throws Exception {
        return mvc.perform(get("/match/events/" + event + "/candidates").header(USER, caller).header(VERIFIED, "true"));
    }

    private ResultActions block(long caller, long target) throws Exception {
        return mvc.perform(post("/match/blocks/" + target).header(USER, caller));
    }

    @Test
    void blockedUsersDisappearFromCandidatesInBothDirections() throws Exception {
        block(1, 2).andExpect(status().isNoContent());

        candidates(1, 10).andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].userId").value(3));
        candidates(2, 10).andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].userId").value(3));
    }

    @Test
    void swipingOnABlockedUserLooksLikeAnUnknownUserWhoeverBlocked() throws Exception {
        block(1, 2).andExpect(status().isNoContent());

        swipe(1, 10, 2).andExpect(status().isNotFound());
        swipe(2, 10, 1).andExpect(status().isNotFound());
        assertThat(swipes.findAll()).isEmpty();
    }

    @Test
    void blockingIsIdempotentAndListedAndReversible() throws Exception {
        block(1, 2).andExpect(status().isNoContent());
        block(1, 2).andExpect(status().isNoContent());

        mvc.perform(get("/match/blocks").header(USER, 1))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].userId").value(2));
        mvc.perform(delete("/match/blocks/2").header(USER, 1)).andExpect(status().isNoContent());
        mvc.perform(get("/match/blocks").header(USER, 1)).andExpect(jsonPath("$.length()").value(0));
        candidates(1, 10).andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void youCannotBlockYourself() throws Exception {
        block(1, 1).andExpect(status().isBadRequest());
    }

    @Test
    void blockingEndsEveryMatchAndClosesTheConversations() throws Exception {
        long m10 = matchOnEvent(10);
        long m11 = matchOnEvent(11);
        long c10 = conversationOf(m10);
        long c11 = conversationOf(m11);
        mvc.perform(post("/chat/" + c10 + "/messages").header(USER, 1)
                .contentType(MediaType.APPLICATION_JSON).content("{\"content\":\"hi\"}")).andExpect(status().isOk());

        block(2, 1).andExpect(status().isNoContent());

        mvc.perform(get("/match/matches").header(USER, 1)).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get("/match/matches").header(USER, 2)).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get("/chat/conversations").header(USER, 1)).andExpect(jsonPath("$.length()").value(0));
        for (long conversation : new long[]{c10, c11}) {
            mvc.perform(get("/chat/" + conversation).header(USER, 1)).andExpect(status().isGone());
            mvc.perform(get("/chat/" + conversation + "/messages").header(USER, 2)).andExpect(status().isGone());
            mvc.perform(post("/chat/" + conversation + "/messages").header(USER, 1)
                    .contentType(MediaType.APPLICATION_JSON).content("{\"content\":\"still there?\"}")).andExpect(status().isGone());
            mvc.perform(post("/chat/" + conversation + "/unlock").header(USER, 1)).andExpect(status().isGone());
        }
        assertThat(matches.findById(m10).orElseThrow().getEndedBy()).isEqualTo(2L);
    }

    @Test
    void unmatchEndsOnlyThatMatchAndOnlyParticipantsCanDoIt() throws Exception {
        long m10 = matchOnEvent(10);
        long m11 = matchOnEvent(11);

        mvc.perform(delete("/match/matches/" + m10).header(USER, 3)).andExpect(status().isForbidden());
        mvc.perform(delete("/match/matches/999999").header(USER, 1)).andExpect(status().isNotFound());
        mvc.perform(delete("/match/matches/" + m10).header(USER, 1)).andExpect(status().isNoContent());
        mvc.perform(delete("/match/matches/" + m10).header(USER, 1)).andExpect(status().isNoContent());

        mvc.perform(get("/match/matches").header(USER, 2))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(m11));
        mvc.perform(get("/chat/" + conversationOf(m10)).header(USER, 2)).andExpect(status().isGone());
        mvc.perform(get("/chat/" + conversationOf(m11)).header(USER, 2)).andExpect(status().isOk());
        // The pair cannot be matched again on that event.
        swipe(1, 10, 2).andExpect(jsonPath("$.matched").value(false));
    }

    @Test
    void reportsAreStoredValidatedAndCanAlsoBlock() throws Exception {
        String body = "{\"reportedUserId\":2,\"reason\":\"HARASSMENT\",\"note\":\"rude messages\",\"eventId\":10,\"block\":true}";
        mvc.perform(post("/match/reports").header(USER, 1).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reporterId").value(1))
                .andExpect(jsonPath("$.reportedUserId").value(2))
                .andExpect(jsonPath("$.status").value("OPEN"));

        assertThat(blocks.existsBetween(1L, 2L)).isTrue();
        mvc.perform(post("/match/reports").header(USER, 1).contentType(MediaType.APPLICATION_JSON)
                .content("{\"reportedUserId\":1,\"reason\":\"SPAM\"}")).andExpect(status().isBadRequest());
        mvc.perform(post("/match/reports").header(USER, 1).contentType(MediaType.APPLICATION_JSON)
                .content("{\"reportedUserId\":2,\"reason\":\"NOT_A_REASON\"}")).andExpect(status().isBadRequest());
        mvc.perform(post("/match/reports").header(USER, 1).contentType(MediaType.APPLICATION_JSON)
                .content("{\"reportedUserId\":2}")).andExpect(status().isBadRequest());
    }

    @Test
    void onlyAdminsReadAndResolveReports() throws Exception {
        mvc.perform(post("/match/reports").header(USER, 1).contentType(MediaType.APPLICATION_JSON)
                .content("{\"reportedUserId\":2,\"reason\":\"FAKE_PROFILE\"}")).andExpect(status().isCreated());
        long id = reports.findAll().get(0).getId();

        mvc.perform(get("/match/admin/reports").header(USER, 9)).andExpect(status().isForbidden());
        mvc.perform(get("/match/admin/reports").header(USER, 9).header("X-User-Role", "USER")).andExpect(status().isForbidden());
        mvc.perform(post("/match/admin/reports/" + id + "/resolve").header(USER, 9)).andExpect(status().isForbidden());

        mvc.perform(get("/match/admin/reports").header(USER, 9).header("X-User-Role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].reason").value("FAKE_PROFILE"));
        mvc.perform(post("/match/admin/reports/" + id + "/resolve").header(USER, 9).header("X-User-Role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"));
        mvc.perform(get("/match/admin/reports").header(USER, 9).header("X-User-Role", "ADMIN"))
                .andExpect(jsonPath("$.length()").value(0));
    }
}
