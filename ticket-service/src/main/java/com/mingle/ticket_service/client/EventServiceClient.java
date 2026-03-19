package com.mingle.ticket_service.client;

import com.mingle.ticket_service.client.dto.response.EventResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "event-service")
public interface EventServiceClient {

    @GetMapping("/event/{id}")
    ResponseEntity<EventResponse> findById(@PathVariable("id") Long eventId);

}
