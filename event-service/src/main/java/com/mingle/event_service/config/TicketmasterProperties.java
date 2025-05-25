package com.mingle.event_service.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "ticketmaster.api")
public class TicketmasterProperties {
    private String baseUrl;
    private String key;
    private int size;
    private String importCron;
    private int retryMaxAttempts;
    private int retryDefaultDelaySeconds;
    private List<String> cities;
}
