package com.mingle.match_service.repository;

import com.mingle.match_service.entity.Match;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MatchRepository extends JpaRepository<Match, Long> {

    Optional<Match> findByEventIdAndUserOneAndUserTwo(Long eventId, Long userOne, Long userTwo);

    @Query("select m from Match m where m.userOne = :userId or m.userTwo = :userId order by m.createdAt desc")
    List<Match> findForUser(@Param("userId") Long userId);

    @Query("select m from Match m where m.eventId = :eventId and (m.userOne = :userId or m.userTwo = :userId) order by m.createdAt desc")
    List<Match> findForUserAndEvent(@Param("userId") Long userId, @Param("eventId") Long eventId);

    /** Returns 1 if the row was inserted, 0 if the pair already matched (safe under concurrent swipes). */
    @Modifying
    @Query(value = "insert into matches (event_id, user_one, user_two) values (:eventId, :userOne, :userTwo) on conflict do nothing",
            nativeQuery = true)
    int insertIfAbsent(@Param("eventId") Long eventId, @Param("userOne") Long userOne, @Param("userTwo") Long userTwo);
}
