package com.apexon.stayease.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.apexon.stayease.model.RoomRequest;
import com.apexon.stayease.model.RoomResponse;
import com.apexon.stayease.security.CustomUserDetails;
import com.apexon.stayease.service.RoomService;

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
 * Room Controller
 * REST endpoints for room management operations.
 *
 */
@RestController
@RequiredArgsConstructor
@Slf4j
@Validated
@Tag(name = "Rooms", description = "Room management APIs")
public class RoomController {

    private final RoomService roomService;

    @GetMapping("/api/hotels/{id}/rooms")
    @Operation(
            summary = "Get available rooms for a hotel",
            description =
                    "Retrieve available rooms for a hotel within specified date range. Public endpoint - no authentication required.")
    @ApiResponses(
            value = {
                @ApiResponse(
                        responseCode = "200",
                        description = "Available rooms retrieved successfully",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema = @Schema(implementation = RoomResponse.class))),
                @ApiResponse(
                        responseCode = "404",
                        description = "Hotel not found",
                        content = @Content)
            })
    public ResponseEntity<List<RoomResponse>> getAvailableRooms(
            @Parameter(
                            description = "Hotel unique identifier",
                            required = true,
                            example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
                    @PathVariable("id")
                    UUID hotelId,
            @Parameter(
                            description = "Check-in date (YYYY-MM-DD)",
                            required = true,
                            example = "2026-09-01")
                    @RequestParam
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate checkIn,
            @Parameter(
                            description = "Check-out date (YYYY-MM-DD)",
                            required = true,
                            example = "2026-09-05")
                    @RequestParam
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate checkOut) {
        log.info(
                "GET /api/hotels/{}/rooms - Fetching available rooms from {} to {}",
                hotelId,
                checkIn,
                checkOut);
        List<RoomResponse> availableRooms =
                roomService.getAvailableRooms(hotelId, checkIn, checkOut);
        return ResponseEntity.ok(availableRooms);
    }

    @GetMapping("/api/rooms/{id}")
    @Operation(
            summary = "Get room by ID",
            description =
                    "Retrieve detailed information about a specific room. Public endpoint - no authentication required.")
    @ApiResponses(
            value = {
                @ApiResponse(
                        responseCode = "200",
                        description = "Room retrieved successfully",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema = @Schema(implementation = RoomResponse.class))),
                @ApiResponse(
                        responseCode = "404",
                        description = "Room not found",
                        content = @Content)
            })
    public ResponseEntity<RoomResponse> getRoomById(
            @Parameter(
                            description = "Room unique identifier",
                            required = true,
                            example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
                    @PathVariable("id")
                    UUID roomId) {
        log.info("GET /api/rooms/{} - Fetching room details", roomId);
        RoomResponse room = roomService.getRoomById(roomId);
        return ResponseEntity.ok(room);
    }

    @PostMapping("/api/hotels/{id}/rooms")
    @PreAuthorize("hasAnyAuthority('MANAGER', 'ADMIN')")
    @Operation(
            summary = "Create a new room",
            description = "Create a new room for a hotel. Requires MANAGER or ADMIN role.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(
            value = {
                @ApiResponse(
                        responseCode = "201",
                        description = "Room created successfully",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema = @Schema(implementation = RoomResponse.class))),
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
                        description = "Forbidden - MANAGER/ADMIN role required",
                        content = @Content),
                @ApiResponse(
                        responseCode = "404",
                        description = "Hotel not found",
                        content = @Content)
            })
    public ResponseEntity<RoomResponse> createRoom(
            @Parameter(
                            description = "Hotel unique identifier",
                            required = true,
                            example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
                    @PathVariable("id")
                    UUID hotelId,
            @Parameter(description = "Room creation request", required = true) @Valid @RequestBody
                    RoomRequest roomRequest,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails) {
        log.info(
                "POST /api/hotels/{}/rooms - Creating new room: {}",
                hotelId,
                roomRequest.getRoomNumber());
        RoomResponse createdRoom =
                roomService.createRoom(
                        hotelId, roomRequest, userDetails.getUserId(), userDetails.getRole());
        return ResponseEntity.status(HttpStatus.CREATED).body(createdRoom);
    }

    @PutMapping("/api/rooms/{id}")
    @PreAuthorize("hasAnyAuthority('MANAGER', 'ADMIN')")
    @Operation(
            summary = "Update a room",
            description = "Update an existing room's information. Requires MANAGER or ADMIN role.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(
            value = {
                @ApiResponse(
                        responseCode = "200",
                        description = "Room updated successfully",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema = @Schema(implementation = RoomResponse.class))),
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
                        description = "Forbidden - MANAGER/ADMIN role required",
                        content = @Content),
                @ApiResponse(
                        responseCode = "404",
                        description = "Room not found",
                        content = @Content)
            })
    public ResponseEntity<RoomResponse> updateRoom(
            @Parameter(
                            description = "Room unique identifier",
                            required = true,
                            example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
                    @PathVariable("id")
                    UUID roomId,
            @Parameter(description = "Room update request", required = true) @Valid @RequestBody
                    RoomRequest roomRequest,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails) {
        log.info("PUT /api/rooms/{} - Updating room", roomId);
        RoomResponse updatedRoom =
                roomService.updateRoom(
                        roomId, roomRequest, userDetails.getUserId(), userDetails.getRole());
        return ResponseEntity.ok(updatedRoom);
    }

    @DeleteMapping("/api/rooms/{id}")
    @PreAuthorize("hasAnyAuthority('MANAGER', 'ADMIN')")
    @Operation(
            summary = "Delete a room",
            description = "Delete a room from the system. Requires MANAGER or ADMIN role.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(
            value = {
                @ApiResponse(responseCode = "204", description = "Room deleted successfully"),
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
                        description = "Room not found",
                        content = @Content)
            })
    public ResponseEntity<Void> deleteRoom(
            @Parameter(
                            description = "Room unique identifier",
                            required = true,
                            example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
                    @PathVariable("id")
                    UUID roomId,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails) {
        log.info("DELETE /api/rooms/{} - Deleting room", roomId);
        roomService.deleteRoom(roomId, userDetails.getUserId(), userDetails.getRole());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/api/rooms/{id}/status")
    @PreAuthorize("hasAnyAuthority('MANAGER', 'ADMIN')")
    @Operation(
            summary = "Toggle room availability status",
            description =
                    "Update room availability status (active/inactive). Requires MANAGER or ADMIN role.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(
            value = {
                @ApiResponse(
                        responseCode = "200",
                        description = "Room status updated successfully",
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
                        description = "Room not found",
                        content = @Content)
            })
    public ResponseEntity<RoomResponse> updateRoomStatus(
            @Parameter(
                            description = "Room unique identifier",
                            required = true,
                            example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
                    @PathVariable("id")
                    UUID roomId,
            @Parameter(description = "New availability status", required = true, example = "false")
                    @RequestParam
                    Boolean isAvailable,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails) {
        log.info("PATCH /api/rooms/{}/status - Updating room status to {}", roomId, isAvailable);
        RoomResponse updatedRoom =
                roomService.updateRoomStatus(
                        roomId, isAvailable, userDetails.getUserId(), userDetails.getRole());
        return ResponseEntity.ok(updatedRoom);
    }
}
