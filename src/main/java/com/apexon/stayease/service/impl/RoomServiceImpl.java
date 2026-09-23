package com.apexon.stayease.service.impl;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.apexon.stayease.entity.Booking;
import com.apexon.stayease.entity.Hotel;
import com.apexon.stayease.entity.Room;
import com.apexon.stayease.enums.BookingStatus;
import com.apexon.stayease.exception.ResourceNotFoundException;
import com.apexon.stayease.model.RoomRequest;
import com.apexon.stayease.model.RoomResponse;
import com.apexon.stayease.repository.BookingRepository;
import com.apexon.stayease.repository.HotelRepository;
import com.apexon.stayease.repository.RoomRepository;
import com.apexon.stayease.service.RoomService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Room Service Implementation
 * Implements business logic for Room management.
 *
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RoomServiceImpl implements RoomService {

    private final RoomRepository roomRepository;
    private final HotelRepository hotelRepository;
    private final BookingRepository bookingRepository;

    /**
     * Ensures a MANAGER caller owns the given hotel before allowing a mutation. ADMIN callers are
     * always allowed regardless of hotel ownership.
     *
     * @param hotel hotel being modified
     * @param currentUserId authenticated caller's user id
     * @param currentUserRole authenticated caller's role
     */
    private void assertManagerOwnsHotel(Hotel hotel, UUID currentUserId, String currentUserRole) {
        if ("ADMIN".equals(currentUserRole)) {
            return;
        }
        if (!hotel.getManager().getUserId().equals(currentUserId)) {
            throw new AccessDeniedException(
                    "You are not authorized to manage rooms for this hotel");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoomResponse> getAvailableRooms(
            UUID hotelId, LocalDate checkInDate, LocalDate checkOutDate) {
        log.info(
                "Fetching available rooms for hotel: {} from {} to {}",
                hotelId,
                checkInDate,
                checkOutDate);

        // Validate hotel exists
        Hotel hotel =
                hotelRepository
                        .findById(hotelId)
                        .orElseThrow(
                                () -> new ResourceNotFoundException("Hotel", "hotelId", hotelId));

        // Validate dates
        if (checkInDate == null || checkOutDate == null) {
            throw new IllegalArgumentException("Check-in and check-out dates are required");
        }

        if (checkOutDate.isBefore(checkInDate) || checkOutDate.isEqual(checkInDate)) {
            throw new IllegalArgumentException("Check-out date must be after check-in date");
        }

        // Get all rooms for the hotel
        List<Room> allRooms = roomRepository.findByHotelIdAndIsAvailable(hotelId, true);

        // Filter out rooms with conflicting bookings
        List<BookingStatus> excludeStatuses = List.of(BookingStatus.CANCELLED);

        return allRooms.stream()
                .filter(
                        room -> {
                            List<Booking> conflictingBookings =
                                    bookingRepository.findConflictingBookings(
                                            room.getRoomId(),
                                            checkInDate,
                                            checkOutDate,
                                            excludeStatuses);
                            return conflictingBookings.isEmpty();
                        })
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoomResponse> getRoomsByHotel(UUID hotelId) {
        log.info("Fetching all rooms for hotel: {}", hotelId);

        // Validate hotel exists
        hotelRepository
                .findById(hotelId)
                .orElseThrow(() -> new ResourceNotFoundException("Hotel", "hotelId", hotelId));

        List<Room> rooms = roomRepository.findByHotelId(hotelId);
        return rooms.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public RoomResponse getRoomById(UUID roomId) {
        log.info("Fetching room with ID: {}", roomId);
        Room room =
                roomRepository
                        .findById(roomId)
                        .orElseThrow(() -> new ResourceNotFoundException("Room", "roomId", roomId));
        return mapToResponse(room);
    }

    @Override
    @Transactional
    public RoomResponse createRoom(
            UUID hotelId, RoomRequest roomRequest, UUID currentUserId, String currentUserRole) {
        log.info("Creating new room: {} for hotel: {}", roomRequest.getRoomNumber(), hotelId);

        // Validate hotel exists
        Hotel hotel =
                hotelRepository
                        .findById(hotelId)
                        .orElseThrow(
                                () -> new ResourceNotFoundException("Hotel", "hotelId", hotelId));

        assertManagerOwnsHotel(hotel, currentUserId, currentUserRole);

        // Check if room number already exists in this hotel
        roomRepository
                .findByHotelIdAndRoomNumber(hotelId, roomRequest.getRoomNumber())
                .ifPresent(
                        existingRoom -> {
                            throw new IllegalArgumentException(
                                    String.format(
                                            "Room number %s already exists in hotel %s",
                                            roomRequest.getRoomNumber(), hotel.getHotelName()));
                        });

        Room room =
                Room.builder()
                        .roomNumber(roomRequest.getRoomNumber())
                        .roomType(roomRequest.getRoomType())
                        .roomDescription(roomRequest.getRoomDescription())
                        .pricePerNight(roomRequest.getPricePerNight())
                        .maxOccupancy(roomRequest.getMaxOccupancy())
                        .roomImageUrl(roomRequest.getRoomImageUrl())
                        .isAvailable(
                                roomRequest.getIsAvailable() != null
                                        ? roomRequest.getIsAvailable()
                                        : true)
                        .hotel(hotel)
                        .build();

        Room savedRoom = roomRepository.save(room);
        log.info("Room created successfully with ID: {}", savedRoom.getRoomId());
        return mapToResponse(savedRoom);
    }

    @Override
    @Transactional
    public RoomResponse updateRoom(
            UUID roomId, RoomRequest roomRequest, UUID currentUserId, String currentUserRole) {
        log.info("Updating room with ID: {}", roomId);

        Room room =
                roomRepository
                        .findById(roomId)
                        .orElseThrow(() -> new ResourceNotFoundException("Room", "roomId", roomId));

        assertManagerOwnsHotel(room.getHotel(), currentUserId, currentUserRole);

        // Check if new room number conflicts with existing room
        if (!room.getRoomNumber().equals(roomRequest.getRoomNumber())) {
            roomRepository
                    .findByHotelIdAndRoomNumber(
                            room.getHotel().getHotelId(), roomRequest.getRoomNumber())
                    .ifPresent(
                            existingRoom -> {
                                throw new IllegalArgumentException(
                                        String.format(
                                                "Room number %s already exists in this hotel",
                                                roomRequest.getRoomNumber()));
                            });
        }

        // Update fields
        room.setRoomNumber(roomRequest.getRoomNumber());
        room.setRoomType(roomRequest.getRoomType());
        room.setRoomDescription(roomRequest.getRoomDescription());
        room.setPricePerNight(roomRequest.getPricePerNight());
        room.setMaxOccupancy(roomRequest.getMaxOccupancy());
        room.setRoomImageUrl(roomRequest.getRoomImageUrl());

        if (roomRequest.getIsAvailable() != null) {
            room.setIsAvailable(roomRequest.getIsAvailable());
        }

        Room updatedRoom = roomRepository.save(room);
        log.info("Room updated successfully with ID: {}", updatedRoom.getRoomId());
        return mapToResponse(updatedRoom);
    }

    @Override
    @Transactional
    public void deleteRoom(UUID roomId, UUID currentUserId, String currentUserRole) {
        log.info("Deleting room with ID: {}", roomId);

        Room room =
                roomRepository
                        .findById(roomId)
                        .orElseThrow(() -> new ResourceNotFoundException("Room", "roomId", roomId));

        assertManagerOwnsHotel(room.getHotel(), currentUserId, currentUserRole);

        // Check if there are any active bookings for this room
        List<Booking> activeBookings =
                bookingRepository.findByRoomId(roomId).stream()
                        .filter(
                                booking ->
                                        booking.getBookingStatus() == BookingStatus.CONFIRMED
                                                || booking.getBookingStatus()
                                                        == BookingStatus.PENDING)
                        .filter(booking -> !booking.getCheckOutDate().isBefore(LocalDate.now()))
                        .collect(Collectors.toList());

        if (!activeBookings.isEmpty()) {
            throw new IllegalStateException(
                    "Cannot delete room with active or future bookings. Please cancel or complete all bookings first.");
        }

        roomRepository.delete(room);
        log.info("Room deleted successfully with ID: {}", roomId);
    }

    @Override
    @Transactional
    public RoomResponse updateRoomStatus(
            UUID roomId, Boolean isAvailable, UUID currentUserId, String currentUserRole) {
        log.info("Updating room status for ID: {} to {}", roomId, isAvailable);

        Room room =
                roomRepository
                        .findById(roomId)
                        .orElseThrow(() -> new ResourceNotFoundException("Room", "roomId", roomId));

        assertManagerOwnsHotel(room.getHotel(), currentUserId, currentUserRole);

        room.setIsAvailable(isAvailable);
        Room updatedRoom = roomRepository.save(room);
        log.info("Room status updated successfully with ID: {}", updatedRoom.getRoomId());
        return mapToResponse(updatedRoom);
    }

    /**
     * Maps Room entity to RoomResponse DTO
     *
     * @param room Room entity
     * @return RoomResponse DTO
     */
    private RoomResponse mapToResponse(Room room) {
        return RoomResponse.builder()
                .roomId(room.getRoomId())
                .roomNumber(room.getRoomNumber())
                .roomType(room.getRoomType())
                .roomDescription(room.getRoomDescription())
                .pricePerNight(room.getPricePerNight())
                .maxOccupancy(room.getMaxOccupancy())
                .roomImageUrl(room.getRoomImageUrl())
                .isAvailable(room.getIsAvailable())
                .hotelId(room.getHotel().getHotelId())
                .hotelName(room.getHotel().getHotelName())
                .createdAt(room.getCreatedAt())
                .updatedAt(room.getUpdatedAt())
                .build();
    }
}
