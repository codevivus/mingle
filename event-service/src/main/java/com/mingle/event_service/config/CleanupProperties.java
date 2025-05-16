package com.mingle.event_service.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "cleanup")
public class CleanupProperties {
    private String cron;
    private int expiredDays;
}
