package com.mingle.api_gateway.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/fallback")
@Tag(name = "Fallback", description = "Fallback endpoints triggered when services are unavailable")
public class FallbackController {

    @Operation(
            summary = "Fallback for an unavailable service",
            description = "Reached through the circuit breaker forward, for any HTTP method"
    )
    @ApiResponse(responseCode = "503", description = "The service is down")
    @RequestMapping("/{service}")
    public ResponseEntity<String> fallback(@PathVariable String service) {
        String name = service.substring(0, 1).toUpperCase() + service.substring(1);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(name + " service is temporarily unavailable");
    }
}
