package com.mingle.event_service.scheduler;

import com.mingle.event_service.service.EventService;
import com.mingle.event_service.service.TicketmasterCityDiscoveryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class TicketmasterImportScheduler {

    private final EventService eventService;
    private final TicketmasterCityDiscoveryService cityDiscoveryService;

    @Scheduled(cron = "${ticketmaster.api.import-cron}")
    public void importEventsFromCities() {
        List<String> cities = cityDiscoveryService.getAvailableCities();

        for (String city : cities) {
            try {
                log.info("Import started for city: {}", city);
                eventService.importEventsFromTicketmaster(city);
                log.info("Import finished for city: {}", city);
            } catch (Exception e) {
                log.error("Import failed for city '{}': {}", city, e.getMessage());
            }
        }
    }
}
