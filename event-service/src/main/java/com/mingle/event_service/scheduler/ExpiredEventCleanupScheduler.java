package com.mingle.event_service.scheduler;

import com.mingle.event_service.config.CleanupProperties;
import com.mingle.event_service.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class ExpiredEventCleanupScheduler {

    private final EventRepository repository;
    private final CleanupProperties cleanupProperties;

    @Scheduled(cron = "${cleanup.cron}")
    public void deleteExpiredImportedEvents() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(cleanupProperties.getExpiredDays());
        int count = repository.deleteExpiredImported(threshold);
        log.info("Deleted {} expired imported events", count);
    }
}
