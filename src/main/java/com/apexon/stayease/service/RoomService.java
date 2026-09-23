package com.apexon.stayease.service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.apexon.stayease.model.RoomRequest;
import com.apexon.stayease.model.RoomResponse;

/**
 * Room Service Interface
 * Defines business operations for Room management.
 *
 */
public interface RoomService {

    /**
     * Get available rooms for a hotel within a date range
     *
     * @param hotelId hotel's unique identifier
     * @param checkInDate check-in date
     * @param checkOutDate check-out date
     * @return List of available rooms
     */
    List<RoomResponse> getAvailableRooms(
            UUID hotelId, LocalDate checkInDate, LocalDate checkOutDate);

    /**
     * Get all rooms for a hotel, regardless of availability status. Used by hotel
     * managers/admins to manage the full room inventory (not just what's bookable).
     *
     * @param hotelId hotel's unique identifier
     * @return List of all rooms in the hotel
     */
    List<RoomResponse> getRoomsByHotel(UUID hotelId);

    /**
     * Get room by ID
     *
     * @param roomId room's unique identifier
     * @return Room details
     */
    RoomResponse getRoomById(UUID roomId);

    /**
     * Create a new room for a hotel. If the caller has the MANAGER role (not ADMIN), they must
     * be the manager assigned to the hotel.
     *
     * @param hotelId hotel's unique identifier
     * @param roomRequest room creation request
     * @param currentUserId authenticated caller's user id
     * @param currentUserRole authenticated caller's role (MANAGER/ADMIN)
     * @return Created room details
     */
    RoomResponse createRoom(
            UUID hotelId, RoomRequest roomRequest, UUID currentUserId, String currentUserRole);

    /**
     * Update an existing room. If the caller has the MANAGER role (not ADMIN), they must be the
     * manager assigned to the room's hotel.
     *
     * @param roomId room's unique identifier
     * @param roomRequest room update request
     * @param currentUserId authenticated caller's user id
     * @param currentUserRole authenticated caller's role (MANAGER/ADMIN)
     * @return Updated room details
     */
    RoomResponse updateRoom(
            UUID roomId, RoomRequest roomRequest, UUID currentUserId, String currentUserRole);

    /**
     * Delete a room. If the caller has the MANAGER role (not ADMIN), they must be the manager
     * assigned to the room's hotel.
     *
     * @param roomId room's unique identifier
     * @param currentUserId authenticated caller's user id
     * @param currentUserRole authenticated caller's role (MANAGER/ADMIN)
     */
    void deleteRoom(UUID roomId, UUID currentUserId, String currentUserRole);

    /**
     * Toggle room availability status. If the caller has the MANAGER role (not ADMIN), they must
     * be the manager assigned to the room's hotel.
     *
     * @param roomId room's unique identifier
     * @param isAvailable new availability status
     * @param currentUserId authenticated caller's user id
     * @param currentUserRole authenticated caller's role (MANAGER/ADMIN)
     * @return Updated room details
     */
    RoomResponse updateRoomStatus(
            UUID roomId, Boolean isAvailable, UUID currentUserId, String currentUserRole);
}
