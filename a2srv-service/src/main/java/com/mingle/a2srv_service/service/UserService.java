package com.mingle.a2srv_service.service;

import com.mingle.a2srv_service.dto.UserRequest;
import com.mingle.a2srv_service.dto.UserResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserService {

    UserResponse register(UserRequest request);

    UserResponse getCurrentUser(String token);

    UserResponse updateProfile(String token, UserRequest request);

    void updatePassword(String token, String oldPassword, String newPassword);

    boolean isEmailTaken(String email);

    boolean isUsernameTaken(String username);

    Page<UserResponse> findByCity(String city, Pageable pageable);

    Page<UserResponse> findByGender(String gender, Pageable pageable);

    Page<UserResponse> findByDatePreference(String pref, Pageable pageable);

    Page<UserResponse> findByRelationshipGoal(String goal, Pageable pageable);

    Page<UserResponse> findByAgeRange(int min, int max, Pageable pageable);

    Page<UserResponse> findVerifiedUsers(Pageable pageable);

    Page<UserResponse> findOnlineUsers(Pageable pageable);

    Page<UserResponse> findLatestActiveUsers(Pageable pageable);

    Page<UserResponse> findNearbyUsers(float lat, float lon, double radius, Pageable pageable);
}
