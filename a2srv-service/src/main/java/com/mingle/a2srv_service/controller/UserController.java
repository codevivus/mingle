package com.mingle.a2srv_service.controller;

import com.mingle.a2srv_service.dto.UserRequest;
import com.mingle.a2srv_service.dto.UserResponse;
import com.mingle.a2srv_service.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@RequestBody UserRequest request) {
        return ResponseEntity.ok(userService.register(request));
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getMe(@RequestHeader("Authorization") String token) {
        return ResponseEntity.ok(userService.getCurrentUser(token));
    }

    @PutMapping("/me")
    public ResponseEntity<UserResponse> updateMe(@RequestHeader("Authorization") String token,
                                                 @RequestBody UserRequest request) {
        return ResponseEntity.ok(userService.updateProfile(token, request));
    }

    @PutMapping("/password")
    public ResponseEntity<Void> updatePassword(@RequestHeader("Authorization") String token,
                                               @RequestParam String oldPassword,
                                               @RequestParam String newPassword) {
        userService.updatePassword(token, oldPassword, newPassword);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/check-email")
    public ResponseEntity<Boolean> checkEmail(@RequestParam String value) {
        return ResponseEntity.ok(userService.isEmailTaken(value));
    }

    @GetMapping("/check-username")
    public ResponseEntity<Boolean> checkUsername(@RequestParam String value) {
        return ResponseEntity.ok(userService.isUsernameTaken(value));
    }

    // 🔍 Gelişmiş Arama Endpoint'leri

    @GetMapping("/search/city")
    public ResponseEntity<Page<UserResponse>> searchByCity(@RequestParam String city, Pageable pageable) {
        return ResponseEntity.ok(userService.findByCity(city, pageable));
    }

    @GetMapping("/search/gender")
    public ResponseEntity<Page<UserResponse>> searchByGender(@RequestParam String gender, Pageable pageable) {
        return ResponseEntity.ok(userService.findByGender(gender, pageable));
    }

    @GetMapping("/search/date-preference")
    public ResponseEntity<Page<UserResponse>> searchByDatePreference(@RequestParam String value, Pageable pageable) {
        return ResponseEntity.ok(userService.findByDatePreference(value, pageable));
    }

    @GetMapping("/search/goal")
    public ResponseEntity<Page<UserResponse>> searchByGoal(@RequestParam String value, Pageable pageable) {
        return ResponseEntity.ok(userService.findByRelationshipGoal(value, pageable));
    }

    @GetMapping("/search/age-range")
    public ResponseEntity<Page<UserResponse>> searchByAgeRange(
            @RequestParam int min,
            @RequestParam int max,
            Pageable pageable
    ) {
        return ResponseEntity.ok(userService.findByAgeRange(min, max, pageable));
    }

    @GetMapping("/search/verified")
    public ResponseEntity<Page<UserResponse>> searchVerifiedUsers(Pageable pageable) {
        return ResponseEntity.ok(userService.findVerifiedUsers(pageable));
    }

    @GetMapping("/search/online")
    public ResponseEntity<Page<UserResponse>> searchOnlineUsers(Pageable pageable) {
        return ResponseEntity.ok(userService.findOnlineUsers(pageable));
    }

    @GetMapping("/search/recent")
    public ResponseEntity<Page<UserResponse>> searchRecentUsers(Pageable pageable) {
        return ResponseEntity.ok(userService.findLatestActiveUsers(pageable));
    }

    @GetMapping("/search/nearby")
    public ResponseEntity<Page<UserResponse>> searchNearbyUsers(
            @RequestParam float lat,
            @RequestParam float lon,
            @RequestParam double radius,
            Pageable pageable
    ) {
        return ResponseEntity.ok(userService.findNearbyUsers(lat, lon, radius, pageable));
    }
}
