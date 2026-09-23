package com.apexon.stayease.service.impl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.apexon.stayease.entity.Booking;
import com.apexon.stayease.entity.Hotel;
import com.apexon.stayease.entity.Room;
import com.apexon.stayease.entity.User;
import com.apexon.stayease.enums.BookingStatus;
import com.apexon.stayease.exception.ResourceNotFoundException;
import com.apexon.stayease.model.BookingRequest;
import com.apexon.stayease.model.BookingResponse;
import com.apexon.stayease.repository.BookingRepository;
import com.apexon.stayease.repository.HotelRepository;
import com.apexon.stayease.repository.RoomRepository;
import com.apexon.stayease.repository.UserRepository;
import com.apexon.stayease.service.BookingService;
import com.apexon.stayease.util.BookingReferenceGenerator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Booking Service Implementation
 * Implements business logic for Booking management.
 *
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final RoomRepository roomRepository;
    private final UserRepository userRepository;
    private final HotelRepository hotelRepository;

    @Override
    @Transactional
    public BookingResponse createBooking(BookingRequest bookingRequest, UUID customerId) {
        log.info(
                "Creating booking for customer: {} and room: {}",
                customerId,
                bookingRequest.getRoomId());

        // Validate customer exists
        User customer =
                userRepository
                        .findById(customerId)
                        .orElseThrow(
                                () -> new ResourceNotFoundException("User", "userId", customerId));

        // Validate room exists
        Room room =
                roomRepository
                        .findById(bookingRequest.getRoomId())
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Room", "roomId", bookingRequest.getRoomId()));

        // Validate room is available
        if (!room.getIsAvailable()) {
            throw new IllegalStateException("Room is currently not available for booking");
        }

        // Validate dates
        validateBookingDates(bookingRequest.getCheckInDate(), bookingRequest.getCheckOutDate());

        // Validate number of guests
        if (bookingRequest.getNumberOfGuests() > room.getMaxOccupancy()) {
            throw new IllegalArgumentException(
                    String.format(
                            "Number of guests (%d) exceeds room's maximum occupancy (%d)",
                            bookingRequest.getNumberOfGuests(), room.getMaxOccupancy()));
        }

        // Check for conflicting bookings
        List<BookingStatus> excludeStatuses = List.of(BookingStatus.CANCELLED);
        List<Booking> conflictingBookings =
                bookingRepository.findConflictingBookings(
                        room.getRoomId(),
                        bookingRequest.getCheckInDate(),
                        bookingRequest.getCheckOutDate(),
                        excludeStatuses);

        if (!conflictingBookings.isEmpty()) {
            throw new IllegalStateException("Room is not available for the selected dates");
        }

        // Calculate total amount
        long numberOfNights =
                ChronoUnit.DAYS.between(
                        bookingRequest.getCheckInDate(), bookingRequest.getCheckOutDate());
        BigDecimal totalAmount =
                room.getPricePerNight().multiply(BigDecimal.valueOf(numberOfNights));

        // Generate unique booking reference
        String bookingReference;
        do {
            bookingReference = BookingReferenceGenerator.generate();
        } while (bookingRepository.existsByBookingReference(bookingReference));

        // Create booking
        Booking booking =
                Booking.builder()
                        .bookingReference(bookingReference)
                        .checkInDate(bookingRequest.getCheckInDate())
                        .checkOutDate(bookingRequest.getCheckOutDate())
                        .numberOfGuests(bookingRequest.getNumberOfGuests())
                        .totalAmount(totalAmount)
                        .bookingStatus(BookingStatus.CONFIRMED)
                        .specialRequests(bookingRequest.getSpecialRequests())
                        .customer(customer)
                        .room(room)
                        .build();

        Booking savedBooking = bookingRepository.save(booking);
        log.info(
                "Booking created successfully with reference: {}",
                savedBooking.getBookingReference());
        return mapToResponse(savedBooking);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingResponse> getMyBookings(UUID customerId) {
        log.info("Fetching bookings for customer: {}", customerId);

        // Validate customer exists
        userRepository
                .findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "userId", customerId));

        List<Booking> bookings = bookingRepository.findByCustomerId(customerId);
        return bookings.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public BookingResponse cancelBooking(UUID bookingId, UUID customerId) {
        log.info("Cancelling booking: {} for customer: {}", bookingId, customerId);

        // Validate booking exists
        Booking booking =
                bookingRepository
                        .findById(bookingId)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Booking", "bookingId", bookingId));

        // Validate booking belongs to customer
        if (!booking.getCustomer().getUserId().equals(customerId)) {
            throw new IllegalArgumentException("You are not authorized to cancel this booking");
        }

        // Validate booking can be cancelled
        if (booking.getBookingStatus() == BookingStatus.CANCELLED) {
            throw new IllegalStateException("Booking is already cancelled");
        }

        if (booking.getBookingStatus() == BookingStatus.COMPLETED) {
            throw new IllegalStateException("Cannot cancel a completed booking");
        }

        // Check if cancellation is allowed (e.g., not too close to check-in date)
        LocalDate today = LocalDate.now();
        if (booking.getCheckInDate().isBefore(today)) {
            throw new IllegalStateException("Cannot cancel a booking with past check-in date");
        }

        // Update booking status
        booking.setBookingStatus(BookingStatus.CANCELLED);
        Booking updatedBooking = bookingRepository.save(booking);
        log.info("Booking cancelled successfully: {}", updatedBooking.getBookingReference());
        return mapToResponse(updatedBooking);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingResponse> getManagerBookings(UUID managerId) {
        log.info("Fetching bookings for manager: {}", managerId);

        // Validate manager exists
        User manager =
                userRepository
                        .findById(managerId)
                        .orElseThrow(
                                () -> new ResourceNotFoundException("User", "userId", managerId));

        // Get hotels managed by this manager
        List<Hotel> managedHotels = hotelRepository.findByManagerId(managerId);

        if (managedHotels.isEmpty()) {
            log.warn("Manager {} has no hotels assigned", managerId);
            return List.of();
        }

        // Get upcoming bookings for all managed hotels
        LocalDate today = LocalDate.now();
        List<Booking> upcomingBookings =
                managedHotels.stream()
                        .flatMap(
                                hotel ->
                                        bookingRepository
                                                .findByHotelId(hotel.getHotelId())
                                                .stream())
                        .filter(
                                booking ->
                                        booking.getCheckInDate().isAfter(today)
                                                || booking.getCheckInDate().isEqual(today))
                        .filter(
                                booking ->
                                        booking.getBookingStatus() == BookingStatus.CONFIRMED
                                                || booking.getBookingStatus()
                                                        == BookingStatus.PENDING)
                        .sorted((b1, b2) -> b1.getCheckInDate().compareTo(b2.getCheckInDate()))
                        .collect(Collectors.toList());

        return upcomingBookings.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    /**
     * Validates booking dates
     *
     * @param checkInDate check-in date
     * @param checkOutDate check-out date
     */
    private void validateBookingDates(LocalDate checkInDate, LocalDate checkOutDate) {
        if (checkInDate == null || checkOutDate == null) {
            throw new IllegalArgumentException("Check-in and check-out dates are required");
        }

        LocalDate today = LocalDate.now();
        if (checkInDate.isBefore(today)) {
            throw new IllegalArgumentException("Check-in date cannot be in the past");
        }

        if (checkOutDate.isBefore(checkInDate) || checkOutDate.isEqual(checkInDate)) {
            throw new IllegalArgumentException("Check-out date must be after check-in date");
        }

        // Optional: Add maximum booking duration validation
        long numberOfNights = ChronoUnit.DAYS.between(checkInDate, checkOutDate);
        if (numberOfNights > 30) {
            throw new IllegalArgumentException("Booking duration cannot exceed 30 nights");
        }
    }

    /**
     * Maps Booking entity to BookingResponse DTO
     *
     * @param booking Booking entity
     * @return BookingResponse DTO
     */
    private BookingResponse mapToResponse(Booking booking) {
        Room room = booking.getRoom();
        Hotel hotel = room.getHotel();
        User customer = booking.getCustomer();

        return BookingResponse.builder()
                .bookingId(booking.getBookingId())
                .bookingReference(booking.getBookingReference())
                .checkInDate(booking.getCheckInDate())
                .checkOutDate(booking.getCheckOutDate())
                .numberOfGuests(booking.getNumberOfGuests())
                .totalAmount(booking.getTotalAmount())
                .bookingStatus(booking.getBookingStatus())
                .specialRequests(booking.getSpecialRequests())
                .customerId(customer.getUserId())
                .customerName(customer.getFullName())
                .customerEmail(customer.getEmailAddress())
                .roomId(room.getRoomId())
                .roomNumber(room.getRoomNumber())
                .roomType(room.getRoomType())
                .hotelId(hotel.getHotelId())
                .hotelName(hotel.getHotelName())
                .createdAt(booking.getCreatedAt())
                .updatedAt(booking.getUpdatedAt())
                .build();
    }
}
