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
class UserDataTest {

    private static final String USER = "X-User-Id";
    private static final String KEY = "test-internal-key-0123456789";

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
    void setUp() throws Exception {
        messages.deleteAll();
        conversations.deleteAll();
        matches.deleteAll();
        swipes.deleteAll();
        blocks.deleteAll();
        reports.deleteAll();
        when(ticketClient.holders(10L)).thenReturn(List.of(1L, 2L, 3L));
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Europe/Istanbul"));
        when(eventClient.findById(10L)).thenReturn(new EventInfo(10L, now, now.plusDays(1)));

        // 1 and 2 match and talk; 1 and 3 match and talk; 2 reports 1; 1 reports 3; 3 blocks 2.
        match(1, 2);
        match(1, 3);
        for (var c : conversations.findAll()) {
            say(c.getUserOne(), c.getId(), "from " + c.getUserOne());
            say(c.getUserTwo(), c.getId(), "from " + c.getUserTwo());
        }
        report(2, 1);
        report(1, 3);
        mvc.perform(post("/match/blocks/2").header(USER, 3)).andExpect(status().isNoContent());
    }

    private void match(long a, long b) throws Exception {
        for (long[] pair : new long[][]{{a, b}, {b, a}}) {
            mvc.perform(post("/match/events/10/swipe").header(USER, pair[0]).header("X-User-Verified", "true")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"targetUserId\":" + pair[1] + ",\"action\":\"LIKE\"}")).andExpect(status().isOk());
        }
    }

    private void say(long user, long conversationId, String text) throws Exception {
        mvc.perform(post("/chat/" + conversationId + "/messages").header(USER, user)
                .contentType(MediaType.APPLICATION_JSON).content("{\"content\":\"" + text + "\"}")).andExpect(status().isOk());
    }

    private void report(long reporter, long reported) throws Exception {
        mvc.perform(post("/match/reports").header(USER, reporter).contentType(MediaType.APPLICATION_JSON)
                .content("{\"reportedUserId\":" + reported + ",\"reason\":\"SPAM\"}")).andExpect(status().isCreated());
    }

    @Test
    void internalEndpointsNeedTheSharedKey() throws Exception {
        mvc.perform(get("/internal/users/1/export")).andExpect(status().isForbidden());
        mvc.perform(get("/internal/users/1/export").header("X-Internal-Key", "wrong")).andExpect(status().isForbidden());
        mvc.perform(delete("/internal/users/1")).andExpect(status().isForbidden());
        assertThat(matches.findAll()).hasSize(2);
    }

    @Test
    void exportContainsOnlyTheUsersOwnData() throws Exception {
        mvc.perform(get("/internal/users/1/export").header("X-Internal-Key", KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.swipes.length()").value(2))
                .andExpect(jsonPath("$.matches.length()").value(2))
                .andExpect(jsonPath("$.messagesSent.length()").value(2))
                .andExpect(jsonPath("$.messagesSent[*].messages[*].content", org.hamcrest.Matchers.everyItem(
                        org.hamcrest.Matchers.is("from 1"))))
                .andExpect(jsonPath("$.reportsFiled.length()").value(1))
                .andExpect(jsonPath("$.blocks.length()").value(0));
        mvc.perform(get("/internal/users/3/export").header("X-Internal-Key", KEY))
                .andExpect(jsonPath("$.blocks.length()").value(1));
    }

    @Test
    void purgeRemovesTheUsersDataEverywhereAndLeavesOthersAlone() throws Exception {
        mvc.perform(delete("/internal/users/1").header("X-Internal-Key", KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matches").value(2))
                .andExpect(jsonPath("$.swipes").value(4))
                .andExpect(jsonPath("$.reportsFiled").value(1));

        assertThat(matches.findAll()).isEmpty();
        assertThat(conversations.findAll()).isEmpty();
        assertThat(messages.findAll()).isEmpty();
        assertThat(swipes.findAll()).isEmpty();
        // Reports about user 1 stay for moderation; the report user 1 filed is gone; 3's block of 2 is untouched.
        assertThat(reports.findAll()).extracting("reported").containsExactly(1L);
        assertThat(blocks.findAll()).hasSize(1);
    }

    @Test
    void purgeIsIdempotent() throws Exception {
        mvc.perform(delete("/internal/users/1").header("X-Internal-Key", KEY)).andExpect(status().isOk());
        mvc.perform(delete("/internal/users/1").header("X-Internal-Key", KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matches").value(0));
    }
}
