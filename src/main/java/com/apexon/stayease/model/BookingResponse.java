package com.apexon.stayease.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import com.apexon.stayease.enums.BookingStatus;
import com.apexon.stayease.enums.RoomType;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Booking Response DTO
 * Response payload for booking information.
 *
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Response payload for booking information")
public class BookingResponse {

    @Schema(
            description = "Booking's unique identifier",
            example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
    private UUID bookingId;

    @Schema(description = "Unique booking reference", example = "BK-20260825-ABCD1234")
    private String bookingReference;

    @Schema(description = "Check-in date", example = "2026-09-01")
    private LocalDate checkInDate;

    @Schema(description = "Check-out date", example = "2026-09-05")
    private LocalDate checkOutDate;

    @Schema(description = "Number of guests", example = "2")
    private Integer numberOfGuests;

    @Schema(description = "Total amount", example = "600.00")
    private BigDecimal totalAmount;

    @Schema(description = "Booking status", example = "CONFIRMED")
    private BookingStatus bookingStatus;

    @Schema(description = "Special requests or notes", example = "Late check-in expected")
    private String specialRequests;

    @Schema(
            description = "Customer's unique identifier",
            example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
    private UUID customerId;

    @Schema(description = "Customer's full name", example = "John Doe")
    private String customerName;

    @Schema(description = "Customer's email", example = "john.doe@example.com")
    private String customerEmail;

    @Schema(
            description = "Room's unique identifier",
            example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
    private UUID roomId;

    @Schema(description = "Room number", example = "101")
    private String roomNumber;

    @Schema(description = "Room type", example = "DELUXE")
    private RoomType roomType;

    @Schema(
            description = "Hotel's unique identifier",
            example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
    private UUID hotelId;

    @Schema(description = "Hotel name", example = "Grand Plaza Hotel")
    private String hotelName;

    @Schema(description = "Booking creation timestamp", example = "2023-08-25T10:30:00")
    private LocalDateTime createdAt;

    @Schema(description = "Last update timestamp", example = "2023-08-25T10:30:00")
    private LocalDateTime updatedAt;
}
