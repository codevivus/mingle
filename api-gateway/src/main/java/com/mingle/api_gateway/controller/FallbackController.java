package com.mingle.api_gateway.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/fallback")
@Tag(name = "Fallback", description = "Fallback endpoints triggered when services are unavailable")
public class FallbackController {

    @Operation(
            summary = "Fallback for auth service",
            description = "Returns a message when the auth service is unavailable"
    )
    @ApiResponse(responseCode = "503", description = "Auth service is down")
    @GetMapping("/auth")
    public ResponseEntity<String> fallbackAuth() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body("Auth service is temporarily unavailable");
    }
}
