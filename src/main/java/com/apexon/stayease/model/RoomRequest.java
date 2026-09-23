package com.apexon.stayease.model;

import java.math.BigDecimal;

import com.apexon.stayease.enums.RoomType;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Room Request DTO
 * Request payload for creating or updating a room.
 *
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request payload for creating or updating a room")
public class RoomRequest {

    @NotBlank(message = "Room number is required")
    @Size(max = 20, message = "Room number must not exceed 20 characters")
    @Schema(description = "Room number", example = "101")
    private String roomNumber;

    @NotNull(message = "Room type is required")
    @Schema(description = "Type of room", example = "DELUXE")
    private RoomType roomType;

    @Schema(description = "Room description", example = "Spacious room with city view")
    private String roomDescription;

    @NotNull(message = "Price per night is required")
    @DecimalMin(value = "0.01", message = "Price per night must be greater than 0")
    @Digits(
            integer = 8,
            fraction = 2,
            message = "Price must have at most 8 integer digits and 2 decimal places")
    @Schema(description = "Price per night", example = "150.00")
    private BigDecimal pricePerNight;

    @NotNull(message = "Max occupancy is required")
    @Min(value = 1, message = "Max occupancy must be at least 1")
    @Schema(description = "Maximum number of guests", example = "2")
    private Integer maxOccupancy;

    @Size(max = 500, message = "Room image URL must not exceed 500 characters")
    @Schema(description = "Room image URL", example = "https://example.com/images/room-101.jpg")
    private String roomImageUrl;

    @Schema(description = "Room availability status", example = "true")
    private Boolean isAvailable;
}
