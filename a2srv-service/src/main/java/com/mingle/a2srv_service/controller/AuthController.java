package com.mingle.a2srv_service.controller;

import com.mingle.a2srv_service.dto.AuthRequest;
import com.mingle.a2srv_service.dto.AuthResponse;
import com.mingle.a2srv_service.dto.ValidateResponse;
import com.mingle.a2srv_service.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody AuthRequest request) {
        return ResponseEntity.ok(authService.authenticate(request));
    }

    @PostMapping("/validate")
    public ResponseEntity<ValidateResponse> validateToken(@RequestHeader("Authorization") String bearerToken) {
        return ResponseEntity.ok(authService.validate(bearerToken));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader("Authorization") String token) {
        authService.logout(token);
        return ResponseEntity.ok().build();
    }
}

