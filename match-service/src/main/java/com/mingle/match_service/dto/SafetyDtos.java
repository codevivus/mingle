package com.mingle.match_service.dto;

import com.mingle.match_service.entity.ReportReason;
import com.mingle.match_service.entity.ReportStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public final class SafetyDtos {

    private SafetyDtos() {
    }

    public record BlockResponse(Long userId, Instant createdAt) {
    }

    /** {@code block} true also blocks the reported user in the same call. */
    public record ReportRequest(@NotNull Long reportedUserId, @NotNull ReportReason reason,
                                @Size(max = 1000) String note, Long eventId, Boolean block) {
    }

    public record ReportResponse(Long id, Long reporterId, Long reportedUserId, Long eventId, ReportReason reason,
                                 String note, ReportStatus status, Instant createdAt) {
    }
}
