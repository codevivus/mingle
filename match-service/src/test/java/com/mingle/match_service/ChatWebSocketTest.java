package com.mingle.match_service;

import com.mingle.match_service.client.EventClient;
import com.mingle.match_service.client.EventClient.EventInfo;
import com.mingle.match_service.client.TicketClient;
import com.mingle.match_service.entity.Conversation;
import com.mingle.match_service.repository.ConversationRepository;
import com.mingle.match_service.repository.MatchRepository;
import com.mingle.match_service.repository.MessageRepository;
import com.mingle.match_service.repository.SwipeRepository;
import com.mingle.match_service.service.MatchService;
import com.mingle.match_service.dto.SwipeRequest;
import com.mingle.match_service.entity.SwipeAction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(PostgresTestConfig.class)
class ChatWebSocketTest {

    @LocalServerPort int port;
    @Autowired MatchService matchService;
    @Autowired ConversationRepository conversations;
    @Autowired MessageRepository messages;
    @Autowired MatchRepository matches;
    @Autowired SwipeRepository swipes;
    @MockBean TicketClient ticketClient;
    @MockBean EventClient eventClient;

    long conversationId;

    @BeforeEach
    void setUp() {
        messages.deleteAll();
        conversations.deleteAll();
        matches.deleteAll();
        swipes.deleteAll();
        when(ticketClient.holders(10L)).thenReturn(List.of(1L, 2L, 3L));
        when(eventClient.findById(10L)).thenReturn(
                new EventInfo(10L, LocalDateTime.now(ZoneOffset.UTC), LocalDateTime.now(ZoneOffset.UTC).plusDays(1)));

        matchService.swipe(1L, 10L, new SwipeRequest(2L, SwipeAction.LIKE));
        matchService.swipe(2L, 10L, new SwipeRequest(1L, SwipeAction.LIKE));
        conversationId = conversations.findAll().stream().findFirst().map(Conversation::getId).orElseThrow();
    }

    private StompSession connect(String userId) throws Exception {
        WebSocketStompClient client = new WebSocketStompClient(new StandardWebSocketClient());
        MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
        converter.getObjectMapper().findAndRegisterModules();
        client.setMessageConverter(converter);

        WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
        if (userId != null) {
            headers.add("X-User-Id", userId);
        }
        return client.connectAsync("ws://localhost:" + port + "/ws", headers, new StompHeaders(),
                new StompSessionHandlerAdapter() { }).get(5, TimeUnit.SECONDS);
    }

    private BlockingQueue<Map<String, Object>> subscribe(StompSession session, String destination) {
        BlockingQueue<Map<String, Object>> received = new LinkedBlockingQueue<>();
        session.subscribe(destination, new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return Map.class;
            }

            @Override
            @SuppressWarnings("unchecked")
            public void handleFrame(StompHeaders headers, Object payload) {
                received.add((Map<String, Object>) payload);
            }
        });
        return received;
    }

    @Test
    void handshakeWithoutUserIdIsRefused() {
        assertThatThrownBy(() -> connect(null)).isInstanceOf(ExecutionException.class);
    }

    @Test
    void messageSentOverStompReachesTheOtherParticipantAndIsStored() throws Exception {
        StompSession sender = connect("1");
        StompSession receiver = connect("2");
        BlockingQueue<Map<String, Object>> inbox = subscribe(receiver, "/topic/conversations/" + conversationId);
        Thread.sleep(500); // let the subscription register before sending

        sender.send("/app/chat/" + conversationId, Map.of("content", "hello over ws"));

        Map<String, Object> message = inbox.poll(5, TimeUnit.SECONDS);
        assertThat(message).isNotNull();
        assertThat(message.get("content")).isEqualTo("hello over ws");
        assertThat(((Number) message.get("senderId")).longValue()).isEqualTo(1L);
        assertThat(messages.findAll()).hasSize(1);
    }

    @Test
    void outsiderCannotSubscribeToAConversation() throws Exception {
        StompSession outsider = connect("3");
        BlockingQueue<Map<String, Object>> inbox = subscribe(outsider, "/topic/conversations/" + conversationId);
        StompSession sender = connect("1");
        Thread.sleep(500);

        sender.send("/app/chat/" + conversationId, Map.of("content", "private"));

        assertThat(inbox.poll(2, TimeUnit.SECONDS)).isNull();
    }

    @Test
    void sendingToALockedChatReturnsAnErrorToTheSenderOnly() throws Exception {
        Conversation c = conversations.findById(conversationId).orElseThrow();
        c.setLockAt(java.time.Instant.now().minusSeconds(60));
        conversations.save(c);

        StompSession sender = connect("1");
        BlockingQueue<Map<String, Object>> errors = subscribe(sender, "/user/queue/errors");
        Thread.sleep(500);

        sender.send("/app/chat/" + conversationId, Map.of("content", "too late"));

        Map<String, Object> error = errors.poll(5, TimeUnit.SECONDS);
        assertThat(error).isNotNull();
        assertThat(((Number) error.get("status")).intValue()).isEqualTo(423);
        assertThat(messages.findAll()).isEmpty();
    }
}
