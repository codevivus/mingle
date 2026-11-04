package com.mingle.match_service.repository;

import com.mingle.match_service.entity.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    Optional<Conversation> findByMatchId(Long matchId);

    @Query("select c from Conversation c where c.userOne = :userId or c.userTwo = :userId order by c.createdAt desc")
    List<Conversation> findForUser(@Param("userId") Long userId);
}
