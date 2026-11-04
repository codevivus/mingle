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

    @Query("select m from Match m where m.userOne = :low and m.userTwo = :high and m.endedAt is null")
    List<Match> findActiveBetween(@Param("low") Long low, @Param("high") Long high);

    @Query("select m from Match m where m.endedAt is null and (m.userOne = :userId or m.userTwo = :userId) order by m.createdAt desc")
    List<Match> findForUser(@Param("userId") Long userId);

    @Query("select m from Match m where m.endedAt is null and m.eventId = :eventId and (m.userOne = :userId or m.userTwo = :userId) order by m.createdAt desc")
    List<Match> findForUserAndEvent(@Param("userId") Long userId, @Param("eventId") Long eventId);

    /** Returns 1 if the row was inserted, 0 if the pair already matched (safe under concurrent swipes). */
    @Modifying
    @Query(value = "insert into matches (event_id, user_one, user_two) values (:eventId, :userOne, :userTwo) on conflict do nothing",
            nativeQuery = true)
    int insertIfAbsent(@Param("eventId") Long eventId, @Param("userOne") Long userOne, @Param("userTwo") Long userTwo);

    /** Every match of the user, ended ones included. */
    @Query("select m from Match m where m.userOne = :userId or m.userTwo = :userId order by m.createdAt asc")
    List<Match> findAllOf(@Param("userId") Long userId);

    /** The database cascades the delete to the conversations and their messages. */
    @Modifying
    @Query("delete from Match m where m.userOne = :userId or m.userTwo = :userId")
    int deleteAllOf(@Param("userId") Long userId);
}
