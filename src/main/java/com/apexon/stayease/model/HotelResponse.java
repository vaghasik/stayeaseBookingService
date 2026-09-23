package com.apexon.stayease.model;

import java.time.LocalDateTime;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Hotel Response DTO
 * Response payload for hotel information.
 *
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Response payload for hotel information")
public class HotelResponse {

    @Schema(
            description = "Hotel's unique identifier",
            example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
    private UUID hotelId;

    @Schema(description = "Hotel name", example = "Grand Plaza Hotel")
    private String hotelName;

    @Schema(
            description = "Hotel description",
            example = "A luxurious 5-star hotel in the heart of the city")
    private String description;

    @Schema(description = "Street address", example = "123 Main Street")
    private String streetAddress;

    @Schema(description = "City name", example = "New York")
    private String cityName;

    @Schema(description = "State name", example = "New York")
    private String stateName;

    @Schema(description = "Country name", example = "USA")
    private String countryName;

    @Schema(description = "Postal code", example = "10001")
    private String postalCode;

    @Schema(description = "Contact phone", example = "+1-212-555-1234")
    private String contactPhone;

    @Schema(description = "Contact email", example = "info@grandplaza.com")
    private String contactEmail;

    @Schema(description = "Star rating (1-5)", example = "5")
    private Integer starRating;

    @Schema(description = "Cover image URL", example = "https://example.com/images/hotel-cover.jpg")
    private String coverImageUrl;

    @Schema(description = "Hotel active status", example = "true")
    private Boolean isActive;

    @Schema(description = "Manager's user ID", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
    private UUID managerId;

    @Schema(description = "Manager's name", example = "John Doe")
    private String managerName;

    @Schema(description = "Creation timestamp", example = "2023-08-25T10:30:00")
    private LocalDateTime createdAt;

    @Schema(description = "Last update timestamp", example = "2023-08-25T10:30:00")
    private LocalDateTime updatedAt;
}
