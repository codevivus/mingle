package com.mingle.ticket_service.client;

import com.mingle.ticket_service.client.dto.response.UserResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "a2srv-service", url = "http://localhost:8051")
public interface UserServiceClient {

    @GetMapping("/user/{id}")
    ResponseEntity<UserResponse> findById(@PathVariable("id") Long userId);

}
