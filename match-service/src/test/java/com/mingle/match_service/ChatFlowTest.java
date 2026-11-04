package com.mingle.match_service;

import com.mingle.match_service.client.EventClient;
import com.mingle.match_service.client.EventClient.EventInfo;
import com.mingle.match_service.client.TicketClient;
import com.mingle.match_service.entity.Conversation;
import com.mingle.match_service.entity.ConversationStatus;
import com.mingle.match_service.repository.ConversationRepository;
import com.mingle.match_service.repository.MatchRepository;
import com.mingle.match_service.repository.MessageRepository;
import com.mingle.match_service.repository.SwipeRepository;
import com.mingle.match_service.unlock.ChatUnlockService;
import com.mingle.match_service.unlock.ChatUnlockService.UnlockResult;
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

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(PostgresTestConfig.class)
class ChatFlowTest {

    private static final String USER = "X-User-Id";

    @Autowired MockMvc mvc;
    @Autowired ConversationRepository conversations;
    @Autowired MessageRepository messages;
    @Autowired MatchRepository matches;
    @Autowired SwipeRepository swipes;
    @MockBean TicketClient ticketClient;
    @MockBean EventClient eventClient;
    @MockBean com.mingle.match_service.client.UserClient userClient;
    @MockBean ChatUnlockService unlockService;

    @BeforeEach
    void setUp() {
        messages.deleteAll();
        conversations.deleteAll();
        matches.deleteAll();
        swipes.deleteAll();
        when(ticketClient.holders(10L)).thenReturn(List.of(1L, 2L, 3L));
        // The event ends tomorrow, so the chat is open.
        when(eventClient.findById(10L)).thenReturn(
                new EventInfo(10L, LocalDateTime.now(ZoneId.of("Europe/Istanbul")), LocalDateTime.now(ZoneId.of("Europe/Istanbul")).plusDays(1)));
        when(unlockService.unlock(any())).thenReturn(UnlockResult.granted("test"));
    }

    private void swipe(long caller, long target) throws Exception {
        mvc.perform(post("/match/events/10/swipe").header(USER, caller)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetUserId\":" + target + ",\"action\":\"LIKE\"}"))
                .andExpect(status().isOk());
    }

    /** Users 1 and 2 match; returns the conversation id. */
    private long matchOneAndTwo() throws Exception {
        swipe(1, 2);
        swipe(2, 1);
        return conversations.findAll().get(0).getId();
    }

    private void send(long caller, long conversationId, String content, int expectedStatus) throws Exception {
        mvc.perform(post("/chat/" + conversationId + "/messages").header(USER, caller)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"" + content + "\"}"))
                .andExpect(status().is(expectedStatus));
    }

    private void lockInThePast(long conversationId) {
        Conversation c = conversations.findById(conversationId).orElseThrow();
        c.setLockAt(Instant.now().minusSeconds(60));
        conversations.save(c);
    }

    @Test
    void matchOpensAnActiveConversationForBothUsers() throws Exception {
        long id = matchOneAndTwo();

        mvc.perform(get("/chat/conversations").header(USER, 1))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(id))
                .andExpect(jsonPath("$[0].otherUserId").value(2))
                .andExpect(jsonPath("$[0].status").value("ACTIVE"));
        mvc.perform(get("/chat/conversations").header(USER, 2))
                .andExpect(jsonPath("$[0].otherUserId").value(1));
        mvc.perform(get("/chat/conversations").header(USER, 3))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void messagesAreStoredAndReturnedOldestFirst() throws Exception {
        long id = matchOneAndTwo();
        send(1, id, "first", 200);
        send(2, id, "second", 200);

        mvc.perform(get("/chat/" + id + "/messages").header(USER, 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].content").value("first"))
                .andExpect(jsonPath("$[0].senderId").value(1))
                .andExpect(jsonPath("$[1].content").value("second"));
    }

    @Test
    void outsidersCannotReadOrWrite() throws Exception {
        long id = matchOneAndTwo();

        mvc.perform(get("/chat/" + id + "/messages").header(USER, 3)).andExpect(status().isForbidden());
        send(3, id, "hi", 403);
        mvc.perform(post("/chat/" + id + "/unlock").header(USER, 3)).andExpect(status().isForbidden());
        mvc.perform(get("/chat/9999/messages").header(USER, 1)).andExpect(status().isNotFound());
    }

    @Test
    void blankMessageIsRejected() throws Exception {
        long id = matchOneAndTwo();
        send(1, id, "   ", 400);
    }

    private void lockReadInThePast(long conversationId) {
        Conversation c = conversations.findById(conversationId).orElseThrow();
        c.setLockAt(Instant.now().minusSeconds(120));
        c.setReadUntil(Instant.now().minusSeconds(60));
        conversations.save(c);
    }

    @Test
    void newConversationLocksSendingOneDayAfterTheEventAndReadingThirtyDaysLater() throws Exception {
        long id = matchOneAndTwo();

        Conversation c = conversations.findById(id).orElseThrow();
        // The mocked event ends in one day; defaults are 24 hours to stop sending and 30 days to stop reading.
        assertThat(c.getLockAt()).isBetween(Instant.now().plusSeconds(47 * 3600), Instant.now().plusSeconds(49 * 3600));
        assertThat(java.time.Duration.between(c.getLockAt(), c.getReadUntil())).isEqualTo(java.time.Duration.ofDays(30));
    }

    @Test
    void afterTheSendWindowChatIsReadOnly() throws Exception {
        long id = matchOneAndTwo();
        send(1, id, "before the lock", 200);

        lockInThePast(id);

        send(1, id, "too late", 423);
        mvc.perform(get("/chat/" + id).header(USER, 1)).andExpect(jsonPath("$.status").value("READ_ONLY"));
        mvc.perform(get("/chat/" + id + "/messages").header(USER, 2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
        assertThat(conversations.findById(id).orElseThrow().getStatus()).isEqualTo(ConversationStatus.READ_ONLY);
    }

    @Test
    void afterTheReadWindowTheHistoryIsLockedToo() throws Exception {
        long id = matchOneAndTwo();
        send(1, id, "old message", 200);

        lockReadInThePast(id);

        mvc.perform(get("/chat/" + id + "/messages").header(USER, 2)).andExpect(status().isLocked());
        send(1, id, "nope", 423);
        mvc.perform(get("/chat/" + id).header(USER, 1)).andExpect(jsonPath("$.status").value("LOCKED"));

        mvc.perform(post("/chat/" + id + "/unlock").header(USER, 1)).andExpect(jsonPath("$.status").value("ACTIVE"));
        mvc.perform(get("/chat/" + id + "/messages").header(USER, 2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
        send(2, id, "welcome back", 200);
    }

    @Test
    void unlockReopensTheChatAndStaysOpen() throws Exception {
        long id = matchOneAndTwo();
        lockInThePast(id);
        send(1, id, "locked", 423);

        mvc.perform(post("/chat/" + id + "/unlock").header(USER, 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        send(2, id, "back again", 200);
        // lock_at is still in the past, but an unlocked chat is not locked again.
        mvc.perform(get("/chat/" + id).header(USER, 1)).andExpect(jsonPath("$.status").value("ACTIVE"));
        // Unlocking an open chat is a no-op.
        mvc.perform(post("/chat/" + id + "/unlock").header(USER, 2)).andExpect(status().isOk());
    }

    @Test
    void unlockWithoutPaymentIsRejectedAndChatStaysLocked() throws Exception {
        long id = matchOneAndTwo();
        lockInThePast(id);
        when(unlockService.unlock(any())).thenReturn(UnlockResult.paymentRequired("https://pay.example/x"));

        mvc.perform(post("/chat/" + id + "/unlock").header(USER, 1)).andExpect(status().isPaymentRequired());
        send(1, id, "still locked", 423);
    }
}
