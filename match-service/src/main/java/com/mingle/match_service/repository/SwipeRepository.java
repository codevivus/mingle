package com.mingle.match_service.repository;

import com.mingle.match_service.entity.Swipe;
import com.mingle.match_service.entity.SwipeAction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SwipeRepository extends JpaRepository<Swipe, Long> {

    Optional<Swipe> findByEventIdAndFromUserAndToUser(Long eventId, Long fromUser, Long toUser);

    boolean existsByEventIdAndFromUserAndToUserAndAction(Long eventId, Long fromUser, Long toUser, SwipeAction action);

    @Query("select s.toUser from Swipe s where s.eventId = :eventId and s.fromUser = :userId")
    List<Long> findSwipedTargets(@Param("eventId") Long eventId, @Param("userId") Long userId);
}
