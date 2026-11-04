package com.mingle.a2srv_service.controller;

import com.mingle.a2srv_service.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/user/me")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    /** Deletes the account and the data other services hold about it. Body: {"password": "..."}. */
    @DeleteMapping
    public ResponseEntity<Void> delete(@RequestHeader("Authorization") String token,
                                       @RequestBody Map<String, String> body) {
        accountService.deleteAccount(token, body.get("password"));
        return ResponseEntity.noContent().build();
    }

    /** Everything stored about the caller, as JSON. */
    @GetMapping("/export")
    public ResponseEntity<Map<String, Object>> export(@RequestHeader("Authorization") String token) {
        return ResponseEntity.ok(accountService.export(token));
    }
}
