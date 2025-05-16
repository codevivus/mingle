package com.mingle.event_service.scheduler;

import com.mingle.event_service.config.TicketmasterProperties;
import com.mingle.event_service.service.TicketmasterCityDiscoveryService;
import com.mingle.event_service.service.TicketmasterEventImporter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class TicketmasterImportScheduler {

    private final TicketmasterEventImporter eventImporter;
    private final TicketmasterCityDiscoveryService cityDiscoveryService;

    @Scheduled(cron = "${ticketmaster.api.import-cron}")
    public void importEventsFromTicketmasterCities() {
        List<String> cities = cityDiscoveryService.getAvailableCities();
        for (String city : cities) {
            importAsync(city);
        }
    }

    @Async("taskExecutor")
    public void importAsync(String city) {
        try {
            log.info("Async import started for city: {}", city);
            eventImporter.importEvents(city);
            log.info("Async import finished for city: {}", city);
        } catch (Exception e) {
            log.warn("Async import failed for city '{}': {}", city, e.getMessage());
        }
    }
}

