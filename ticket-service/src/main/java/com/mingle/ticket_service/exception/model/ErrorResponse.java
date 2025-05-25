package com.mingle.ticket_service.exception.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ErrorResponse {
    private int status;
    private String error;
    private String message;
    private String path;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Default
    private LocalDateTime timestamp = LocalDateTime.now();
}
