package com.mingle.match_service.service;

import com.mingle.match_service.entity.Conversation;
import com.mingle.match_service.repository.BlockRepository;
import com.mingle.match_service.repository.ConversationRepository;
import com.mingle.match_service.repository.MatchRepository;
import com.mingle.match_service.repository.MessageRepository;
import com.mingle.match_service.repository.ReportRepository;
import com.mingle.match_service.repository.SwipeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Everything match-service stores about one user: export (right of access) and purge (right to erasure). */
@Service
@RequiredArgsConstructor
public class UserDataService {

    private final SwipeRepository swipes;
    private final MatchRepository matches;
    private final ConversationRepository conversations;
    private final MessageRepository messages;
    private final BlockRepository blocks;
    private final ReportRepository reports;

    /**
     * The user's own data only: their swipes, matches, the messages they wrote, the blocks they made and the
     * reports they filed. What the other person wrote is that person's data and is not included.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> export(Long userId) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("swipes", swipes.findByFromUserOrderByCreatedAtAsc(userId).stream()
                .map(s -> row("eventId", s.getEventId(), "targetUserId", s.getToUser(), "action", s.getAction(),
                        "createdAt", s.getCreatedAt()))
                .toList());
        out.put("matches", matches.findAllOf(userId).stream()
                .map(m -> row("id", m.getId(), "eventId", m.getEventId(), "otherUserId", m.otherUser(userId),
                        "createdAt", m.getCreatedAt(), "endedAt", m.getEndedAt()))
                .toList());
        out.put("messagesSent", conversations.findForUser(userId).stream()
                .map(c -> conversationMessages(c, userId)).toList());
        out.put("blocks", blocks.findByBlockerOrderByCreatedAtDesc(userId).stream()
                .map(b -> row("userId", b.getBlocked(), "createdAt", b.getCreatedAt())).toList());
        out.put("reportsFiled", reports.findByReporterOrderByCreatedAtAsc(userId).stream()
                .map(r -> row("id", r.getId(), "reportedUserId", r.getReported(), "reason", r.getReason(),
                        "note", r.getNote(), "status", r.getStatus(), "createdAt", r.getCreatedAt()))
                .toList());
        return out;
    }

    /**
     * Deletes the user's matches (the database cascades to conversations and messages), swipes, blocks and the
     * reports they filed. Reports about the user are kept for moderation. Safe to repeat.
     */
    @Transactional
    public Map<String, Integer> purge(Long userId) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        counts.put("matches", matches.deleteAllOf(userId));
        counts.put("swipes", swipes.deleteAllOf(userId));
        counts.put("blocks", blocks.deleteAllOf(userId));
        counts.put("reportsFiled", reports.deleteFiledBy(userId));
        return counts;
    }

    private Map<String, Object> conversationMessages(Conversation c, Long userId) {
        List<Map<String, Object>> mine = messages.findByConversationIdAndSenderIdOrderByIdAsc(c.getId(), userId).stream()
                .map(m -> row("id", m.getId(), "content", m.getContent(), "createdAt", m.getCreatedAt()))
                .toList();
        return row("conversationId", c.getId(), "matchId", c.getMatchId(), "messages", mine);
    }

    private static Map<String, Object> row(Object... keysAndValues) {
        Map<String, Object> row = new LinkedHashMap<>();
        for (int i = 0; i < keysAndValues.length; i += 2) {
            row.put((String) keysAndValues[i], keysAndValues[i + 1]);
        }
        return row;
    }
}
