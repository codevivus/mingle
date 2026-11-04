package com.mingle.match_service.service;

import com.mingle.match_service.dto.SafetyDtos.BlockResponse;
import com.mingle.match_service.dto.SafetyDtos.ReportRequest;
import com.mingle.match_service.dto.SafetyDtos.ReportResponse;
import com.mingle.match_service.entity.Match;
import com.mingle.match_service.entity.Report;
import com.mingle.match_service.entity.ReportStatus;
import com.mingle.match_service.exception.ApiException;
import com.mingle.match_service.repository.BlockRepository;
import com.mingle.match_service.repository.ConversationRepository;
import com.mingle.match_service.repository.MatchRepository;
import com.mingle.match_service.repository.ReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/** Blocking, ending matches and reporting. Ending a match closes its conversation for good. */
@Service
@RequiredArgsConstructor
@Transactional
public class SafetyService {

    private static final int ADMIN_PAGE = 100;

    private final BlockRepository blockRepository;
    private final MatchRepository matchRepository;
    private final ConversationRepository conversationRepository;
    private final ReportRepository reportRepository;

    public void block(Long blocker, Long target) {
        requireOtherUser(blocker, target);
        blockRepository.insertIfAbsent(blocker, target);
        endMatchesBetween(blocker, target, blocker);
    }

    public void unblock(Long blocker, Long target) {
        // Unblocking does not bring ended matches or their conversations back.
        blockRepository.deleteByPair(blocker, target);
    }

    @Transactional(readOnly = true)
    public List<BlockResponse> blocks(Long blocker) {
        return blockRepository.findByBlockerOrderByCreatedAtDesc(blocker).stream()
                .map(b -> new BlockResponse(b.getBlocked(), b.getCreatedAt()))
                .toList();
    }

    public void unmatch(Long caller, Long matchId) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Match not found"));
        if (!match.includes(caller)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "This match belongs to other users");
        }
        end(match, caller);
    }

    public ReportResponse report(Long reporter, ReportRequest request) {
        requireOtherUser(reporter, request.reportedUserId());
        Report saved = reportRepository.save(Report.builder()
                .reporter(reporter)
                .reported(request.reportedUserId())
                .eventId(request.eventId())
                .reason(request.reason())
                .note(request.note())
                .build());
        if (Boolean.TRUE.equals(request.block())) {
            block(reporter, request.reportedUserId());
        }
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ReportResponse> openReports() {
        return reportRepository.findByStatusOrderByCreatedAtAsc(ReportStatus.OPEN, PageRequest.of(0, ADMIN_PAGE))
                .stream().map(this::toResponse).toList();
    }

    public ReportResponse resolve(Long adminId, Long reportId) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Report not found"));
        if (report.getStatus() == ReportStatus.OPEN) {
            report.setStatus(ReportStatus.RESOLVED);
            report.setResolvedAt(Instant.now());
            report.setResolvedBy(adminId);
            reportRepository.save(report);
        }
        return toResponse(report);
    }

    private void endMatchesBetween(Long a, Long b, Long endedBy) {
        matchRepository.findActiveBetween(Math.min(a, b), Math.max(a, b)).forEach(m -> end(m, endedBy));
    }

    private void end(Match match, Long endedBy) {
        Instant now = Instant.now();
        if (match.isActive()) {
            match.setEndedAt(now);
            match.setEndedBy(endedBy);
            matchRepository.save(match);
        }
        conversationRepository.findByMatchId(match.getId()).ifPresent(c -> {
            if (c.getClosedAt() == null) {
                c.setClosedAt(now);
                conversationRepository.save(c);
            }
        });
    }

    private void requireOtherUser(Long caller, Long other) {
        if (caller.equals(other)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "You cannot do this to yourself");
        }
    }

    private ReportResponse toResponse(Report r) {
        return new ReportResponse(r.getId(), r.getReporter(), r.getReported(), r.getEventId(), r.getReason(),
                r.getNote(), r.getStatus(), r.getCreatedAt());
    }
}
