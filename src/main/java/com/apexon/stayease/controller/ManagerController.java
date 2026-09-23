package com.apexon.stayease.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.apexon.stayease.model.BookingResponse;
import com.apexon.stayease.model.HotelResponse;
import com.apexon.stayease.model.RoomResponse;
import com.apexon.stayease.security.CustomUserDetails;
import com.apexon.stayease.service.BookingService;
import com.apexon.stayease.service.HotelService;
import com.apexon.stayease.service.RoomService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Manager Controller
 * REST endpoints for hotel manager operations.
 *
 */
@RestController
@RequestMapping("/api/manager")
@RequiredArgsConstructor
@Slf4j
@Validated
@Tag(name = "Manager", description = "Hotel manager APIs")
public class ManagerController {

    private final BookingService bookingService;
    private final HotelService hotelService;
    private final RoomService roomService;

    @GetMapping("/hotels")
    @PreAuthorize("hasAnyAuthority('MANAGER', 'ADMIN')")
    @Operation(
            summary = "Get hotels managed by the current user",
            description =
                    "Retrieve every hotel whose managerId matches the current user. Requires MANAGER or ADMIN role.",
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
                        description = "Forbidden - MANAGER/ADMIN role required",
                        content = @Content)
            })
    public ResponseEntity<List<HotelResponse>> getMyHotels(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails) {
        log.info(
                "GET /api/manager/hotels - Fetching hotels for manager: {}",
                userDetails.getUserId());
        List<HotelResponse> hotels = hotelService.getHotelsByManagerId(userDetails.getUserId());
        return ResponseEntity.ok(hotels);
    }

    @GetMapping("/hotels/{hotelId}/rooms")
    @PreAuthorize("hasAnyAuthority('MANAGER', 'ADMIN')")
    @Operation(
            summary = "Get all rooms for a hotel (manager view)",
            description =
                    "Retrieve every room for a hotel regardless of availability, for inventory management. Requires MANAGER or ADMIN role.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(
            value = {
                @ApiResponse(
                        responseCode = "200",
                        description = "Rooms retrieved successfully",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema = @Schema(implementation = RoomResponse.class))),
                @ApiResponse(
                        responseCode = "401",
                        description = "Unauthorized - Authentication required",
                        content = @Content),
                @ApiResponse(
                        responseCode = "403",
                        description = "Forbidden - MANAGER/ADMIN role required",
                        content = @Content),
                @ApiResponse(
                        responseCode = "404",
                        description = "Hotel not found",
                        content = @Content)
            })
    public ResponseEntity<List<RoomResponse>> getHotelRooms(
            @Parameter(description = "Hotel unique identifier", required = true) @PathVariable
                    UUID hotelId) {
        log.info("GET /api/manager/hotels/{}/rooms - Fetching all rooms", hotelId);
        List<RoomResponse> rooms = roomService.getRoomsByHotel(hotelId);
        return ResponseEntity.ok(rooms);
    }

    @GetMapping("/bookings")
    @PreAuthorize("hasAnyAuthority('MANAGER', 'ADMIN')")
    @Operation(
            summary = "Get upcoming bookings for manager's hotel",
            description =
                    "Retrieve upcoming bookings for hotels managed by the current user. Requires MANAGER or ADMIN role.",
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
                        description = "Forbidden - MANAGER/ADMIN role required",
                        content = @Content)
            })
    public ResponseEntity<List<BookingResponse>> getManagerBookings(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails) {
        log.info(
                "GET /api/manager/bookings - Fetching bookings for manager: {}",
                userDetails.getUserId());
        List<BookingResponse> bookings = bookingService.getManagerBookings(userDetails.getUserId());
        return ResponseEntity.ok(bookings);
    }
}
