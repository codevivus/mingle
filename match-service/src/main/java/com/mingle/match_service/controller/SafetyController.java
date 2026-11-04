package com.mingle.match_service.controller;

import com.mingle.match_service.dto.SafetyDtos.BlockResponse;
import com.mingle.match_service.dto.SafetyDtos.ReportRequest;
import com.mingle.match_service.dto.SafetyDtos.ReportResponse;
import com.mingle.match_service.exception.ApiException;
import com.mingle.match_service.service.SafetyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/match")
@RequiredArgsConstructor
public class SafetyController {

    private static final String USER_HEADER = "X-User-Id";
    private static final String ROLE_HEADER = "X-User-Role";

    private final SafetyService safetyService;

    @GetMapping("/blocks")
    public List<BlockResponse> blocks(@RequestHeader(USER_HEADER) Long callerId) {
        return safetyService.blocks(callerId);
    }

    @PostMapping("/blocks/{userId}")
    public ResponseEntity<Void> block(@RequestHeader(USER_HEADER) Long callerId, @PathVariable Long userId) {
        safetyService.block(callerId, userId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/blocks/{userId}")
    public ResponseEntity<Void> unblock(@RequestHeader(USER_HEADER) Long callerId, @PathVariable Long userId) {
        safetyService.unblock(callerId, userId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/matches/{matchId}")
    public ResponseEntity<Void> unmatch(@RequestHeader(USER_HEADER) Long callerId, @PathVariable Long matchId) {
        safetyService.unmatch(callerId, matchId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/reports")
    public ResponseEntity<ReportResponse> report(@RequestHeader(USER_HEADER) Long callerId,
                                                 @Valid @RequestBody ReportRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(safetyService.report(callerId, request));
    }

    @GetMapping("/admin/reports")
    public List<ReportResponse> openReports(@RequestHeader(value = ROLE_HEADER, defaultValue = "") String role) {
        requireAdmin(role);
        return safetyService.openReports();
    }

    @PostMapping("/admin/reports/{id}/resolve")
    public ReportResponse resolve(@RequestHeader(USER_HEADER) Long callerId,
                                  @RequestHeader(value = ROLE_HEADER, defaultValue = "") String role,
                                  @PathVariable Long id) {
        requireAdmin(role);
        return safetyService.resolve(callerId, id);
    }

    private static void requireAdmin(String role) {
        if (!"ADMIN".equals(role)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Admin role required");
        }
    }
}
