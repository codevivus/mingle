package com.mingle.match_service.repository;

import com.mingle.match_service.entity.Block;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BlockRepository extends JpaRepository<Block, Long> {

    List<Block> findByBlockerOrderByCreatedAtDesc(Long blocker);

    @Query("select count(b) > 0 from Block b where (b.blocker = :a and b.blocked = :b) or (b.blocker = :b and b.blocked = :a)")
    boolean existsBetween(@Param("a") Long a, @Param("b") Long b);

    /** Everyone this user blocked. */
    @Query("select b.blocked from Block b where b.blocker = :userId")
    List<Long> findBlockedBy(@Param("userId") Long userId);

    /** Everyone who blocked this user. */
    @Query("select b.blocker from Block b where b.blocked = :userId")
    List<Long> findBlockersOf(@Param("userId") Long userId);

    @Modifying
    @Query(value = "insert into blocks (blocker, blocked) values (:blocker, :blocked) on conflict do nothing", nativeQuery = true)
    int insertIfAbsent(@Param("blocker") Long blocker, @Param("blocked") Long blocked);

    @Modifying
    @Query("delete from Block b where b.blocker = :blocker and b.blocked = :blocked")
    int deleteByPair(@Param("blocker") Long blocker, @Param("blocked") Long blocked);
}
