package com.mingle.a2srv_service.controller;

import com.mingle.a2srv_service.dto.UserRequest;
import com.mingle.a2srv_service.dto.UserResponse;
import com.mingle.a2srv_service.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
@Tag(name = "User", description = "User registration, profile operations and user search")
public class UserController {

    private final UserService userService;

    @Operation(summary = "Register a new user", description = "Creates a new user account")
    @ApiResponse(responseCode = "200", description = "User successfully registered")
    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@RequestBody UserRequest request) {
        return ResponseEntity.ok(userService.register(request));
    }

    @Operation(summary = "Get current user profile", description = "Returns the profile of the authenticated user")
    @ApiResponse(responseCode = "200", description = "Current user profile returned")
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getMe(@RequestHeader("Authorization") String token) {
        return ResponseEntity.ok(userService.getCurrentUser(token));
    }

    @Operation(summary = "Update current user profile", description = "Updates the authenticated user's profile")
    @ApiResponse(responseCode = "200", description = "User profile updated successfully")
    @PutMapping("/me")
    public ResponseEntity<UserResponse> updateMe(@RequestHeader("Authorization") String token,
                                                 @RequestBody UserRequest request) {
        return ResponseEntity.ok(userService.updateProfile(token, request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserById(id).get());
    }

    @Operation(summary = "Update password", description = "Changes password for the authenticated user")
    @ApiResponse(responseCode = "200", description = "Password updated successfully")
    @PutMapping("/password")
    public ResponseEntity<Void> updatePassword(@RequestHeader("Authorization") String token,
                                               @RequestParam String oldPassword,
                                               @RequestParam String newPassword) {
        userService.updatePassword(token, oldPassword, newPassword);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Check if email is taken", description = "Returns true if email is already in use")
    @ApiResponse(responseCode = "200", description = "Email availability checked")
    @GetMapping("/check-email")
    public ResponseEntity<Boolean> checkEmail(@RequestParam String value) {
        return ResponseEntity.ok(userService.isEmailTaken(value));
    }

    @Operation(summary = "Check if username is taken", description = "Returns true if username is already in use")
    @ApiResponse(responseCode = "200", description = "Username availability checked")
    @GetMapping("/check-username")
    public ResponseEntity<Boolean> checkUsername(@RequestParam String value) {
        return ResponseEntity.ok(userService.isUsernameTaken(value));
    }

    @Operation(summary = "Search users by city", description = "Returns users located in a specific city")
    @ApiResponse(responseCode = "200", description = "Users found by city")
    @GetMapping("/search/city")
    public ResponseEntity<Page<UserResponse>> searchByCity(@RequestParam String city, Pageable pageable) {
        return ResponseEntity.ok(userService.findByCity(city, pageable));
    }

    @Operation(summary = "Search users by gender", description = "Returns users with the specified gender")
    @ApiResponse(responseCode = "200", description = "Users found by gender")
    @GetMapping("/search/gender")
    public ResponseEntity<Page<UserResponse>> searchByGender(@RequestParam String gender, Pageable pageable) {
        return ResponseEntity.ok(userService.findByGender(gender, pageable));
    }

    @Operation(summary = "Search users by date preference", description = "Returns users by their date preferences")
    @ApiResponse(responseCode = "200", description = "Users found by date preference")
    @GetMapping("/search/date-preference")
    public ResponseEntity<Page<UserResponse>> searchByDatePreference(@RequestParam String value, Pageable pageable) {
        return ResponseEntity.ok(userService.findByDatePreference(value, pageable));
    }

    @Operation(summary = "Search users by goal", description = "Returns users by their relationship goals")
    @ApiResponse(responseCode = "200", description = "Users found by relationship goal")
    @GetMapping("/search/goal")
    public ResponseEntity<Page<UserResponse>> searchByGoal(@RequestParam String value, Pageable pageable) {
        return ResponseEntity.ok(userService.findByRelationshipGoal(value, pageable));
    }

    @Operation(summary = "Search users by age range", description = "Returns users whose age is within the specified range")
    @ApiResponse(responseCode = "200", description = "Users found within age range")
    @GetMapping("/search/age-range")
    public ResponseEntity<Page<UserResponse>> searchByAgeRange(
            @RequestParam int min,
            @RequestParam int max,
            Pageable pageable
    ) {
        return ResponseEntity.ok(userService.findByAgeRange(min, max, pageable));
    }

    @Operation(summary = "Search verified users", description = "Returns all verified users")
    @ApiResponse(responseCode = "200", description = "Verified users returned")
    @GetMapping("/search/verified")
    public ResponseEntity<Page<UserResponse>> searchVerifiedUsers(Pageable pageable) {
        return ResponseEntity.ok(userService.findVerifiedUsers(pageable));
    }

    @Operation(summary = "Search online users", description = "Returns currently online users")
    @ApiResponse(responseCode = "200", description = "Online users returned")
    @GetMapping("/search/online")
    public ResponseEntity<Page<UserResponse>> searchOnlineUsers(Pageable pageable) {
        return ResponseEntity.ok(userService.findOnlineUsers(pageable));
    }

    @Operation(summary = "Search recently active users", description = "Returns users active in the recent period")
    @ApiResponse(responseCode = "200", description = "Recently active users returned")
    @GetMapping("/search/recent")
    public ResponseEntity<Page<UserResponse>> searchRecentUsers(Pageable pageable) {
        return ResponseEntity.ok(userService.findLatestActiveUsers(pageable));
    }

    @Operation(summary = "Search nearby users", description = "Returns users within a specific radius based on coordinates")
    @ApiResponse(responseCode = "200", description = "Nearby users returned")
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
