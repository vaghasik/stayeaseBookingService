package com.apexon.stayease.exception;

import java.time.LocalDateTime;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Error Response DTO
 * Consistent error response format for API errors.
 *
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Standard error response")
public class ErrorResponse {

    @Schema(description = "Timestamp of the error", example = "2026-08-23T10:30:00")
    private LocalDateTime timestamp;

    @Schema(description = "Request path", example = "/api/auth/register")
    private String path;

    @Schema(description = "Error type", example = "Conflict")
    private String error;

    @Schema(description = "Error message", example = "Email already registered")
    private String message;

    @Schema(description = "Additional error details")
    private Map<String, String> details;
}
