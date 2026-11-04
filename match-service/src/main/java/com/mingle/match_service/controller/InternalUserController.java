package com.mingle.match_service.controller;

import com.mingle.match_service.service.UserDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** Called by a2srv-service for account export and deletion. Guarded by InternalKeyFilter; the gateway does not route it. */
@RestController
@RequestMapping("/internal/users")
@RequiredArgsConstructor
public class InternalUserController {

    private final UserDataService userDataService;

    @GetMapping("/{userId}/export")
    public Map<String, Object> export(@PathVariable Long userId) {
        return userDataService.export(userId);
    }

    @DeleteMapping("/{userId}")
    public Map<String, Integer> purge(@PathVariable Long userId) {
        return userDataService.purge(userId);
    }
}
