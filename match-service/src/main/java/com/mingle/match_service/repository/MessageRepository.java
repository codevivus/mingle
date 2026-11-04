package com.mingle.match_service.repository;

import com.mingle.match_service.entity.Message;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {

    /** Newest first; callers reverse the page to show it oldest first. */
    List<Message> findByConversationIdOrderByIdDesc(Long conversationId, Pageable pageable);

    List<Message> findByConversationIdAndSenderIdOrderByIdAsc(Long conversationId, Long senderId);
}
