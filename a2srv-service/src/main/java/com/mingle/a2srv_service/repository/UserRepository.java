package com.mingle.a2srv_service.repository;

import com.mingle.a2srv_service.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);
    Optional<User> findByUsername(String username);

    boolean existsByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByEmailAndIdNot(String email, Long id);
    boolean existsByUsernameAndIdNot(String username, Long id);

    Page<User> findByCity(String city, Pageable pageable);
    Page<User> findByGender(String gender, Pageable pageable);
    Page<User> findByDatePreference(String datePreference, Pageable pageable);
    Page<User> findByRelationshipGoal(String relationshipGoal, Pageable pageable);
    Page<User> findByAgeBetween(int min, int max, Pageable pageable);

    Page<User> findByVerifiedTrue(Pageable pageable);
    Page<User> findByIsOnlineTrue(Pageable pageable);
    Page<User> findAllByOrderByLastActiveAtDesc(Pageable pageable);

    @Query("""
        SELECT u FROM User u 
        WHERE sqrt(power(u.locationLat - :lat, 2) + power(u.locationLon - :lon, 2)) < :radius
    """)
    Page<User> findNearbyUsers(@Param("lat") float lat, @Param("lon") float lon, @Param("radius") double radius, Pageable pageable);
}

