package com.mingle.match_service.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.time.LocalDateTime;

@FeignClient(name = "event-service")
public interface EventClient {

    @GetMapping("/event/{id}")
    EventInfo findById(@PathVariable("id") Long id);

    @JsonIgnoreProperties(ignoreUnknown = true)
    record EventInfo(Long id, LocalDateTime startTime, LocalDateTime endTime) {
    }
}
