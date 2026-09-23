package com.apexon.stayease.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.apexon.stayease.model.BookingRequest;
import com.apexon.stayease.model.BookingResponse;
import com.apexon.stayease.security.CustomUserDetails;
import com.apexon.stayease.service.BookingService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Booking Controller
 * REST endpoints for booking management operations.
 * Testing can be performed using Postman or Swagger UI.
 * Post and Get requests can be tested with appropriate request bodies and parameters.
 */
@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
@Slf4j
@Validated
@Tag(name = "Bookings", description = "Booking management APIs")
public class BookingController {

    private final BookingService bookingService;

    @PostMapping
    @PreAuthorize("hasAuthority('GUEST')")
    @Operation(
            summary = "Create a new booking",
            description = "Create a new room booking. Requires GUEST role.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(
            value = {
                @ApiResponse(
                        responseCode = "201",
                        description = "Booking created successfully",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema = @Schema(implementation = BookingResponse.class))),
                @ApiResponse(
                        responseCode = "400",
                        description = "Invalid request data or room not available",
                        content = @Content),
                @ApiResponse(
                        responseCode = "401",
                        description = "Unauthorized - Authentication required",
                        content = @Content),
                @ApiResponse(
                        responseCode = "403",
                        description = "Forbidden - GUEST role required",
                        content = @Content),
                @ApiResponse(
                        responseCode = "404",
                        description = "Room not found",
                        content = @Content)
            })
    public ResponseEntity<BookingResponse> createBooking(
            @Parameter(description = "Booking creation request", required = true)
                    @Valid
                    @RequestBody
                    BookingRequest bookingRequest,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails) {
        log.info("POST /api/bookings - Creating booking for user: {}", userDetails.getUserId());
        BookingResponse createdBooking =
                bookingService.createBooking(bookingRequest, userDetails.getUserId());
        return ResponseEntity.status(HttpStatus.CREATED).body(createdBooking);
    }

    // Get all bookings for the current user
    @GetMapping("/mine")
    @PreAuthorize("hasAuthority('GUEST')")
    @Operation(
            summary = "Get my bookings",
            description = "Retrieve all bookings for the current user. Requires GUEST role.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(
            value = {
                @ApiResponse(
                        responseCode = "200",
                        description = "Bookings retrieved successfully",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema = @Schema(implementation = BookingResponse.class))),
                @ApiResponse(
                        responseCode = "401",
                        description = "Unauthorized - Authentication required",
                        content = @Content),
                @ApiResponse(
                        responseCode = "403",
                        description = "Forbidden - GUEST role required",
                        content = @Content)
            })
    public ResponseEntity<List<BookingResponse>> getMyBookings(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails) {
        log.info(
                "GET /api/bookings/mine - Fetching bookings for user: {}", userDetails.getUserId());
        List<BookingResponse> bookings = bookingService.getMyBookings(userDetails.getUserId());
        return ResponseEntity.ok(bookings);
    }

    @PutMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('GUEST')")
    @Operation(
            summary = "Cancel a booking",
            description =
                    "Cancel an existing booking. Requires GUEST role and must be booking owner.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(
            value = {
                @ApiResponse(
                        responseCode = "200",
                        description = "Booking cancelled successfully",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema = @Schema(implementation = BookingResponse.class))),
                @ApiResponse(
                        responseCode = "400",
                        description =
                                "Cannot cancel booking (e.g., already cancelled or completed)",
                        content = @Content),
                @ApiResponse(
                        responseCode = "401",
                        description = "Unauthorized - Authentication required",
                        content = @Content),
                @ApiResponse(
                        responseCode = "403",
                        description = "Forbidden - GUEST role required or not booking owner",
                        content = @Content),
                @ApiResponse(
                        responseCode = "404",
                        description = "Booking not found",
                        content = @Content)
            })
    public ResponseEntity<BookingResponse> cancelBooking(
            @Parameter(
                            description = "Booking unique identifier",
                            required = true,
                            example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
                    @PathVariable("id")
                    UUID bookingId,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails) {
        log.info(
                "PUT /api/bookings/{}/cancel - Cancelling booking for user: {}",
                bookingId,
                userDetails.getUserId());
        BookingResponse cancelledBooking =
                bookingService.cancelBooking(bookingId, userDetails.getUserId());
        return ResponseEntity.ok(cancelledBooking);
    }
}
