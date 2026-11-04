package com.mingle.match_service.service;

import com.mingle.match_service.client.EventClient;
import com.mingle.match_service.dto.ConversationResponse;
import com.mingle.match_service.dto.MessageResponse;
import com.mingle.match_service.entity.Conversation;
import com.mingle.match_service.entity.ConversationStatus;
import com.mingle.match_service.entity.Message;
import com.mingle.match_service.exception.ApiException;
import com.mingle.match_service.repository.ConversationRepository;
import com.mingle.match_service.repository.MessageRepository;
import com.mingle.match_service.unlock.ChatUnlockService;
import com.mingle.match_service.unlock.ChatUnlockService.UnlockRequest;
import com.mingle.match_service.unlock.ChatUnlockService.UnlockResult;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ChatService {

    static final int MAX_CONTENT_LENGTH = 2000;

    private final EventClient eventClient;
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final ChatUnlockService unlockService;

    /** Event times carry no zone; they are the local time of the venue, read in this zone. */
    @Value("${chat.event-time-zone:Europe/Istanbul}")
    private String eventTimeZone;

    /** Sending stops this long after the event ends. */
    @Value("${chat.lock-after-hours:24}")
    private long lockAfterHours;

    /** The history stays readable this long after sending stops, then it is locked too. */
    @Value("${chat.read-only-days:30}")
    private long readOnlyDays;

    /**
     * Runs inside the swipe transaction. If event-service is down the swipe fails with 502 and can
     * be retried, so a match never exists without its conversation.
     */
    @EventListener
    public void onMatchCreated(MatchCreatedEvent event) {
        EventClient.EventInfo info = eventClient.findById(event.eventId());
        // Event times are stored without a zone: venue-local time, see chat.event-time-zone.
        var eventDate = info.endTime() != null ? info.endTime() : info.startTime();
        if (eventDate == null) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Event has no date, cannot open a conversation");
        }
        Instant lockAt = eventDate.atZone(ZoneId.of(eventTimeZone)).toInstant().plus(Duration.ofHours(lockAfterHours));
        conversationRepository.save(Conversation.builder()
                .readUntil(lockAt.plus(Duration.ofDays(readOnlyDays)))
                .matchId(event.matchId())
                .eventId(event.eventId())
                .userOne(event.userOne())
                .userTwo(event.userTwo())
                .lockAt(lockAt)
                .build());
    }

    public List<ConversationResponse> conversations(Long callerId) {
        return conversationRepository.findForUser(callerId).stream()
                .filter(c -> c.getClosedAt() == null)
                .map(this::syncStatus)
                .map(c -> toResponse(c, callerId))
                .toList();
    }

    public ConversationResponse conversation(Long callerId, Long conversationId) {
        return toResponse(participantConversation(callerId, conversationId), callerId);
    }

    /** Readable while ACTIVE or READ_ONLY; once LOCKED it needs an unlock. */
    public List<MessageResponse> history(Long callerId, Long conversationId, int page, int size) {
        Conversation conversation = participantConversation(callerId, conversationId);
        if (conversation.getStatus() == ConversationStatus.LOCKED) {
            throw new ApiException(HttpStatus.LOCKED, "This chat is locked, unlock it to read the history");
        }
        List<Message> newestFirst = new ArrayList<>(messageRepository.findByConversationIdOrderByIdDesc(
                conversationId, PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100))));
        Collections.reverse(newestFirst);
        return newestFirst.stream().map(this::toResponse).toList();
    }

    public MessageResponse send(Long callerId, Long conversationId, String content) {
        Conversation conversation = participantConversation(callerId, conversationId);
        if (content == null || content.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Message content must not be blank");
        }
        if (content.length() > MAX_CONTENT_LENGTH) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Message is longer than " + MAX_CONTENT_LENGTH + " characters");
        }
        if (conversation.getStatus() != ConversationStatus.ACTIVE) {
            throw new ApiException(HttpStatus.LOCKED, "This chat is closed for new messages, unlock it to keep talking");
        }
        return toResponse(messageRepository.save(Message.builder()
                .conversationId(conversationId).senderId(callerId).content(content).build()));
    }

    public ConversationResponse unlock(Long callerId, Long conversationId) {
        Conversation conversation = participantConversation(callerId, conversationId);
        if (conversation.getStatus() == ConversationStatus.ACTIVE) {
            return toResponse(conversation, callerId);
        }
        UnlockResult result = unlockService.unlock(
                new UnlockRequest(conversationId, conversation.getEventId(), callerId));
        if (!result.granted()) {
            String where = result.redirectUrl() != null ? ": " + result.redirectUrl() : "";
            throw new ApiException(HttpStatus.PAYMENT_REQUIRED, "Payment required to unlock this chat" + where);
        }
        conversation.setStatus(ConversationStatus.ACTIVE);
        conversation.setUnlockedAt(Instant.now());
        return toResponse(conversationRepository.save(conversation), callerId);
    }

    /**
     * Marks the history as read up to {@code upToMessageId}, or up to the newest message when null. It never
     * moves backwards and never past the newest message.
     */
    public ConversationResponse markRead(Long callerId, Long conversationId, Long upToMessageId) {
        Conversation conversation = participantConversation(callerId, conversationId);
        long newest = messageRepository.findFirstByConversationIdOrderByIdDesc(conversationId)
                .map(Message::getId).orElse(0L);
        long target = upToMessageId == null ? newest : Math.min(Math.max(upToMessageId, 0L), newest);
        conversation.markRead(callerId, target);
        return toResponse(conversationRepository.save(conversation), callerId);
    }

    /** Unread messages across every open conversation, for a badge. */
    @Transactional(readOnly = true)
    public long unreadTotal(Long callerId) {
        return conversationRepository.findForUser(callerId).stream()
                .filter(c -> c.getClosedAt() == null)
                .mapToLong(c -> messageRepository.countByConversationIdAndIdGreaterThanAndSenderIdNot(
                        c.getId(), c.lastReadBy(callerId), callerId))
                .sum();
    }

    /** Used by the STOMP layer to authorise subscriptions. */
    @Transactional(readOnly = true)
    public void assertParticipant(Long userId, Long conversationId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Conversation not found"));
        if (!conversation.hasParticipant(userId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You are not part of this conversation");
        }
        if (conversation.getClosedAt() != null) {
            throw new ApiException(HttpStatus.GONE, "This conversation has ended");
        }
    }

    private Conversation participantConversation(Long callerId, Long conversationId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Conversation not found"));
        if (!conversation.hasParticipant(callerId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You are not part of this conversation");
        }
        if (conversation.getClosedAt() != null) {
            throw new ApiException(HttpStatus.GONE, "This conversation has ended");
        }
        return syncStatus(conversation);
    }

    /** Locks lazily: the first access after a deadline moves the stored status forward. */
    private Conversation syncStatus(Conversation c) {
        if (c.getUnlockedAt() != null) {
            return c; // an unlocked chat is never locked again
        }
        Instant now = Instant.now();
        ConversationStatus due = !now.isBefore(c.getReadUntil()) ? ConversationStatus.LOCKED
                : !now.isBefore(c.getLockAt()) ? ConversationStatus.READ_ONLY
                : ConversationStatus.ACTIVE;
        if (due.ordinal() > c.getStatus().ordinal()) {
            c.setStatus(due);
            return conversationRepository.save(c);
        }
        return c;
    }

    private ConversationResponse toResponse(Conversation c, Long callerId) {
        Long other = c.getUserOne().equals(callerId) ? c.getUserTwo() : c.getUserOne();
        long unread = messageRepository.countByConversationIdAndIdGreaterThanAndSenderIdNot(
                c.getId(), c.lastReadBy(callerId), callerId);
        MessageResponse last = messageRepository.findFirstByConversationIdOrderByIdDesc(c.getId())
                .map(this::toResponse).orElse(null);
        return new ConversationResponse(c.getId(), c.getMatchId(), c.getEventId(), other, c.getStatus(), c.getLockAt(),
                c.getReadUntil(), unread, last, c.lastReadBy(other));
    }

    private MessageResponse toResponse(Message m) {
        return new MessageResponse(m.getId(), m.getConversationId(), m.getSenderId(), m.getContent(), m.getCreatedAt());
    }
}
