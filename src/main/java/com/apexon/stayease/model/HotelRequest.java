package com.apexon.stayease.model;

import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Hotel Request DTO
 * Request payload for creating or updating a hotel.
 *
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request payload for creating or updating a hotel")
public class HotelRequest {

    @NotBlank(message = "Hotel name is required")
    @Size(max = 150, message = "Hotel name must not exceed 150 characters")
    @Schema(description = "Hotel name", example = "Grand Plaza Hotel")
    private String hotelName;

    @Schema(
            description = "Hotel description",
            example = "A luxurious 5-star hotel in the heart of the city")
    private String description;

    @Size(max = 255, message = "Street address must not exceed 255 characters")
    @Schema(description = "Street address", example = "123 Main Street")
    private String streetAddress;

    @NotBlank(message = "City name is required")
    @Size(max = 100, message = "City name must not exceed 100 characters")
    @Schema(description = "City name", example = "New York")
    private String cityName;

    @Size(max = 100, message = "State name must not exceed 100 characters")
    @Schema(description = "State name", example = "New York")
    private String stateName;

    @NotBlank(message = "Country name is required")
    @Size(max = 100, message = "Country name must not exceed 100 characters")
    @Schema(description = "Country name", example = "USA")
    private String countryName;

    @Size(max = 20, message = "Postal code must not exceed 20 characters")
    @Schema(description = "Postal code", example = "10001")
    private String postalCode;

    @Size(max = 20, message = "Contact phone must not exceed 20 characters")
    @Schema(description = "Contact phone", example = "+1-212-555-1234")
    private String contactPhone;

    @Email(message = "Contact email must be valid")
    @Size(max = 150, message = "Contact email must not exceed 150 characters")
    @Schema(description = "Contact email", example = "info@grandplaza.com")
    private String contactEmail;

    @Min(value = 1, message = "Star rating must be at least 1")
    @Max(value = 5, message = "Star rating must not exceed 5")
    @Schema(description = "Star rating (1-5)", example = "5")
    private Integer starRating;

    @Size(max = 500, message = "Cover image URL must not exceed 500 characters")
    @Schema(description = "Cover image URL", example = "https://example.com/images/hotel-cover.jpg")
    private String coverImageUrl;

    @NotNull(message = "Manager ID is required")
    @Schema(description = "Manager's user ID", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
    private UUID managerId;

    @Schema(description = "Hotel active status", example = "true")
    private Boolean isActive;
}
