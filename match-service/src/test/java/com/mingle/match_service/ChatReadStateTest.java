package com.mingle.match_service;

import com.mingle.match_service.client.EventClient;
import com.mingle.match_service.client.EventClient.EventInfo;
import com.mingle.match_service.client.TicketClient;
import com.mingle.match_service.client.UserClient;
import com.mingle.match_service.repository.BlockRepository;
import com.mingle.match_service.repository.ConversationRepository;
import com.mingle.match_service.repository.MatchRepository;
import com.mingle.match_service.repository.MessageRepository;
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

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(PostgresTestConfig.class)
class ChatReadStateTest {

    private static final String USER = "X-User-Id";

    @Autowired MockMvc mvc;
    @Autowired MatchRepository matches;
    @Autowired SwipeRepository swipes;
    @Autowired ConversationRepository conversations;
    @Autowired MessageRepository messages;
    @Autowired BlockRepository blocks;
    @MockBean TicketClient ticketClient;
    @MockBean EventClient eventClient;
    @MockBean UserClient userClient;

    long conversationId;

    @BeforeEach
    void setUp() throws Exception {
        messages.deleteAll();
        conversations.deleteAll();
        matches.deleteAll();
        swipes.deleteAll();
        blocks.deleteAll();
        when(ticketClient.holders(10L)).thenReturn(List.of(1L, 2L, 3L));
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Europe/Istanbul"));
        when(eventClient.findById(10L)).thenReturn(new EventInfo(10L, now, now.plusDays(1)));
        when(userClient.findById(anyLong())).thenThrow(new RuntimeException("no profiles here"));

        for (long[] pair : new long[][]{{1, 2}, {2, 1}}) {
            mvc.perform(post("/match/events/10/swipe").header(USER, pair[0]).header("X-User-Verified", "true")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"targetUserId\":" + pair[1] + ",\"action\":\"LIKE\"}")).andExpect(status().isOk());
        }
        conversationId = conversations.findAll().get(0).getId();
    }

    private long send(long caller, String content) throws Exception {
        mvc.perform(post("/chat/" + conversationId + "/messages").header(USER, caller)
                .contentType(MediaType.APPLICATION_JSON).content("{\"content\":\"" + content + "\"}"))
                .andExpect(status().isOk());
        return messages.findAll().stream().mapToLong(m -> m.getId()).max().orElseThrow();
    }

    @Test
    void unreadCountsOnlyTheOtherUsersMessagesAndShowsTheLastOne() throws Exception {
        send(1, "one");
        send(1, "two");
        send(2, "reply");

        mvc.perform(get("/chat/conversations").header(USER, 2))
                .andExpect(jsonPath("$[0].unreadCount").value(2))
                .andExpect(jsonPath("$[0].lastMessage.content").value("reply"));
        mvc.perform(get("/chat/conversations").header(USER, 1))
                .andExpect(jsonPath("$[0].unreadCount").value(1));
        mvc.perform(get("/chat/unread-count").header(USER, 2)).andExpect(jsonPath("$.count").value(2));
    }

    @Test
    void markingReadClearsTheCountAndShowsTheReceiptToTheSender() throws Exception {
        long first = send(1, "one");
        long second = send(1, "two");

        mvc.perform(post("/chat/" + conversationId + "/read").header(USER, 2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(0));

        mvc.perform(get("/chat/" + conversationId).header(USER, 1))
                .andExpect(jsonPath("$.otherLastReadMessageId").value(second));
        mvc.perform(get("/chat/unread-count").header(USER, 2)).andExpect(jsonPath("$.count").value(0));
        org.junit.jupiter.api.Assertions.assertTrue(second > first);
    }

    @Test
    void readingUpToAMessageLeavesTheLaterOnesUnread() throws Exception {
        long first = send(1, "one");
        send(1, "two");
        send(1, "three");

        mvc.perform(post("/chat/" + conversationId + "/read").header(USER, 2)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"upToMessageId\":" + first + "}"))
                .andExpect(jsonPath("$.unreadCount").value(2));
        // It never moves backwards.
        mvc.perform(post("/chat/" + conversationId + "/read").header(USER, 2)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"upToMessageId\":0}"))
                .andExpect(jsonPath("$.unreadCount").value(2))
                .andExpect(jsonPath("$.otherLastReadMessageId").value(0));
    }

    @Test
    void aMessageIdBeyondTheNewestIsClamped() throws Exception {
        send(1, "one");

        mvc.perform(post("/chat/" + conversationId + "/read").header(USER, 2)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"upToMessageId\":999999999}"))
                .andExpect(jsonPath("$.unreadCount").value(0));
        send(1, "after");
        mvc.perform(get("/chat/conversations").header(USER, 2)).andExpect(jsonPath("$[0].unreadCount").value(1));
    }

    @Test
    void outsidersAndClosedConversationsCannotBeMarkedRead() throws Exception {
        mvc.perform(post("/chat/" + conversationId + "/read").header(USER, 3)).andExpect(status().isForbidden());

        mvc.perform(post("/match/blocks/2").header(USER, 1)).andExpect(status().isNoContent());
        mvc.perform(post("/chat/" + conversationId + "/read").header(USER, 1)).andExpect(status().isGone());
        mvc.perform(get("/chat/unread-count").header(USER, 1)).andExpect(jsonPath("$.count").value(0));
    }
}
