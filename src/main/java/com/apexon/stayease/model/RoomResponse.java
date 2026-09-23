package com.apexon.stayease.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import com.apexon.stayease.enums.RoomType;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Room Response DTO
 * Response payload for room information.
 *
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Response payload for room information")
public class RoomResponse {

    @Schema(
            description = "Room's unique identifier",
            example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
    private UUID roomId;

    @Schema(description = "Room number", example = "101")
    private String roomNumber;

    @Schema(description = "Type of room", example = "DELUXE")
    private RoomType roomType;

    @Schema(description = "Room description", example = "Spacious room with city view")
    private String roomDescription;

    @Schema(description = "Price per night", example = "150.00")
    private BigDecimal pricePerNight;

    @Schema(description = "Maximum number of guests", example = "2")
    private Integer maxOccupancy;

    @Schema(description = "Room image URL", example = "https://example.com/images/room-101.jpg")
    private String roomImageUrl;

    @Schema(description = "Room availability status", example = "true")
    private Boolean isAvailable;

    @Schema(
            description = "Hotel's unique identifier",
            example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
    private UUID hotelId;

    @Schema(description = "Hotel name", example = "Grand Plaza Hotel")
    private String hotelName;

    @Schema(description = "Creation timestamp", example = "2023-08-25T10:30:00")
    private LocalDateTime createdAt;

    @Schema(description = "Last update timestamp", example = "2023-08-25T10:30:00")
    private LocalDateTime updatedAt;
}
