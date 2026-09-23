package com.apexon.stayease.model;

import java.time.LocalDate;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Booking Request DTO
 * Request payload for creating a booking.
 *
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request payload for creating a booking")
public class BookingRequest {

    @NotNull(message = "Room ID is required")
    @Schema(
            description = "Room's unique identifier",
            example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
    private UUID roomId;

    @NotNull(message = "Check-in date is required")
    @FutureOrPresent(message = "Check-in date must be today or in the future")
    @Schema(description = "Check-in date", example = "2026-09-01")
    private LocalDate checkInDate;

    @NotNull(message = "Check-out date is required")
    @Future(message = "Check-out date must be in the future")
    @Schema(description = "Check-out date", example = "2026-09-05")
    private LocalDate checkOutDate;

    @NotNull(message = "Number of guests is required")
    @Min(value = 1, message = "Number of guests must be at least 1")
    @Schema(description = "Number of guests", example = "2")
    private Integer numberOfGuests;

    @Schema(description = "Special requests or notes", example = "Late check-in expected")
    private String specialRequests;
}
