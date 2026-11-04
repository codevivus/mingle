package com.mingle.a2srv_service.controller;

import com.mingle.a2srv_service.service.EmailVerificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class EmailVerificationController {

    private final EmailVerificationService verificationService;

    @GetMapping("/verify")
    public ResponseEntity<Map<String, Boolean>> verify(@RequestParam("token") String token) {
        verificationService.verify(token);
        return ResponseEntity.ok(Map.of("verified", true));
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<Void> resend(@RequestBody Map<String, String> body) {
        verificationService.resend(body.get("email"));
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }
}
