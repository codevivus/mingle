package com.mingle.match_service.repository;

import com.mingle.match_service.entity.Message;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {

    /** Newest first; callers reverse the page to show it oldest first. */
    List<Message> findByConversationIdOrderByIdDesc(Long conversationId, Pageable pageable);

    java.util.Optional<Message> findFirstByConversationIdOrderByIdDesc(Long conversationId);

    /** Messages the other participant sent after the given message id. */
    long countByConversationIdAndIdGreaterThanAndSenderIdNot(Long conversationId, Long afterId, Long senderId);
}
