package com.apexon.stayease.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.apexon.stayease.model.HotelRequest;
import com.apexon.stayease.model.HotelResponse;
import com.apexon.stayease.service.HotelService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Hotel Controller
 * REST endpoints for hotel management operations.
 *
 */
@RestController
@RequestMapping("/api/hotels")
@RequiredArgsConstructor
@Slf4j
@Validated
@Tag(name = "Hotels", description = "Hotel management APIs")
public class HotelController {

    private final HotelService hotelService;

    @GetMapping
    @Operation(
            summary = "Get hotels by city",
            description =
                    "Retrieve a list of hotels in a specified city. Public endpoint - no authentication required.")
    @ApiResponses(
            value = {
                @ApiResponse(
                        responseCode = "200",
                        description = "Hotels retrieved successfully",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema = @Schema(implementation = HotelResponse.class)))
            })
    public ResponseEntity<List<HotelResponse>> getHotelsByCity(
            @Parameter(description = "City name", required = true, example = "New York")
                    @RequestParam
                    @NotBlank(message = "City name is required")
                    String city) {
        log.info("GET /api/hotels - Fetching hotels for city: {}", city);
        List<HotelResponse> hotels = hotelService.getHotelsByCity(city);
        return ResponseEntity.ok(hotels);
    }

    @GetMapping("/all")
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(
            summary = "Get all hotels",
            description =
                    "Retrieve every hotel in the system, regardless of city. Requires ADMIN role.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(
            value = {
                @ApiResponse(
                        responseCode = "200",
                        description = "Hotels retrieved successfully",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema = @Schema(implementation = HotelResponse.class))),
                @ApiResponse(
                        responseCode = "401",
                        description = "Unauthorized - Authentication required",
                        content = @Content),
                @ApiResponse(
                        responseCode = "403",
                        description = "Forbidden - ADMIN role required",
                        content = @Content)
            })
    public ResponseEntity<List<HotelResponse>> getAllHotels() {
        log.info("GET /api/hotels/all - Fetching all hotels");
        List<HotelResponse> hotels = hotelService.getAllHotels();
        return ResponseEntity.ok(hotels);
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Get hotel by ID",
            description =
                    "Retrieve detailed information about a specific hotel. Public endpoint - no authentication required.")
    @ApiResponses(
            value = {
                @ApiResponse(
                        responseCode = "200",
                        description = "Hotel retrieved successfully",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema = @Schema(implementation = HotelResponse.class))),
                @ApiResponse(
                        responseCode = "404",
                        description = "Hotel not found",
                        content = @Content)
            })
    public ResponseEntity<HotelResponse> getHotelById(
            @Parameter(
                            description = "Hotel unique identifier",
                            required = true,
                            example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
                    @PathVariable
                    UUID id) {
        log.info("GET /api/hotels/{} - Fetching hotel details", id);
        HotelResponse hotel = hotelService.getHotelById(id);
        return ResponseEntity.ok(hotel);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(
            summary = "Create a new hotel",
            description = "Create a new hotel in the system. Requires ADMIN role.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(
            value = {
                @ApiResponse(
                        responseCode = "201",
                        description = "Hotel created successfully",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema = @Schema(implementation = HotelResponse.class))),
                @ApiResponse(
                        responseCode = "400",
                        description = "Invalid request data",
                        content = @Content),
                @ApiResponse(
                        responseCode = "401",
                        description = "Unauthorized - Authentication required",
                        content = @Content),
                @ApiResponse(
                        responseCode = "403",
                        description = "Forbidden - ADMIN role required",
                        content = @Content)
            })
    public ResponseEntity<HotelResponse> createHotel(
            @Parameter(description = "Hotel creation request", required = true) @Valid @RequestBody
                    HotelRequest hotelRequest) {
        log.info("POST /api/hotels - Creating new hotel: {}", hotelRequest.getHotelName());
        HotelResponse createdHotel = hotelService.createHotel(hotelRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdHotel);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(
            summary = "Update a hotel",
            description = "Update an existing hotel's information. Requires ADMIN role.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(
            value = {
                @ApiResponse(
                        responseCode = "200",
                        description = "Hotel updated successfully",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema = @Schema(implementation = HotelResponse.class))),
                @ApiResponse(
                        responseCode = "400",
                        description = "Invalid request data",
                        content = @Content),
                @ApiResponse(
                        responseCode = "401",
                        description = "Unauthorized - Authentication required",
                        content = @Content),
                @ApiResponse(
                        responseCode = "403",
                        description = "Forbidden - ADMIN role required",
                        content = @Content),
                @ApiResponse(
                        responseCode = "404",
                        description = "Hotel not found",
                        content = @Content)
            })
    public ResponseEntity<HotelResponse> updateHotel(
            @Parameter(
                            description = "Hotel unique identifier",
                            required = true,
                            example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
                    @PathVariable
                    UUID id,
            @Parameter(description = "Hotel update request", required = true) @Valid @RequestBody
                    HotelRequest hotelRequest) {
        log.info("PUT /api/hotels/{} - Updating hotel", id);
        HotelResponse updatedHotel = hotelService.updateHotel(id, hotelRequest);
        return ResponseEntity.ok(updatedHotel);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(
            summary = "Delete a hotel",
            description = "Delete a hotel from the system. Requires ADMIN role.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(
            value = {
                @ApiResponse(responseCode = "204", description = "Hotel deleted successfully"),
                @ApiResponse(
                        responseCode = "401",
                        description = "Unauthorized - Authentication required",
                        content = @Content),
                @ApiResponse(
                        responseCode = "403",
                        description = "Forbidden - ADMIN role required",
                        content = @Content),
                @ApiResponse(
                        responseCode = "404",
                        description = "Hotel not found",
                        content = @Content)
            })
    public ResponseEntity<Void> deleteHotel(
            @Parameter(
                            description = "Hotel unique identifier",
                            required = true,
                            example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
                    @PathVariable
                    UUID id) {
        log.info("DELETE /api/hotels/{} - Deleting hotel", id);
        hotelService.deleteHotel(id);
        return ResponseEntity.noContent().build();
    }
}
