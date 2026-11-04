package com.mingle.match_service.controller;

import com.mingle.match_service.dto.CandidateResponse;
import com.mingle.match_service.dto.MatchResponse;
import com.mingle.match_service.dto.SwipeRequest;
import com.mingle.match_service.dto.SwipeResponse;
import com.mingle.match_service.service.MatchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** The caller is the user the gateway authenticated, passed in X-User-Id. */
@RestController
@RequestMapping("/match")
@RequiredArgsConstructor
public class MatchController {

    private static final String USER_HEADER = "X-User-Id";

    private final MatchService matchService;

    @GetMapping("/events/{eventId}/candidates")
    public List<CandidateResponse> candidates(@RequestHeader(USER_HEADER) Long callerId,
                                              @PathVariable Long eventId,
                                              @RequestParam(defaultValue = "50") int limit) {
        return matchService.candidates(callerId, eventId, limit);
    }

    @PostMapping("/events/{eventId}/swipe")
    public SwipeResponse swipe(@RequestHeader(USER_HEADER) Long callerId,
                               @PathVariable Long eventId,
                               @Valid @RequestBody SwipeRequest request) {
        return matchService.swipe(callerId, eventId, request);
    }

    @GetMapping("/matches")
    public List<MatchResponse> matches(@RequestHeader(USER_HEADER) Long callerId,
                                       @RequestParam(required = false) Long eventId) {
        return matchService.matches(callerId, eventId);
    }
}
