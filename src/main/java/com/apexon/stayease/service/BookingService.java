package com.apexon.stayease.service;

import java.util.List;
import java.util.UUID;

import com.apexon.stayease.model.BookingRequest;
import com.apexon.stayease.model.BookingResponse;

/**
 * Booking Service Interface
 * Defines business operations for Booking management.
 *
 */
public interface BookingService {

    /**
     * Create a new booking
     *
     * @param bookingRequest booking creation request
     * @param customerId customer's user ID (from security context)
     * @return Created booking details
     */
    BookingResponse createBooking(BookingRequest bookingRequest, UUID customerId);

    /**
     * Get all bookings for the current user (customer)
     *
     * @param customerId customer's user ID (from security context)
     * @return List of customer's bookings
     */
    List<BookingResponse> getMyBookings(UUID customerId);

    /**
     * Cancel a booking
     *
     * @param bookingId booking's unique identifier
     * @param customerId customer's user ID (from security context)
     * @return Updated booking details
     */
    BookingResponse cancelBooking(UUID bookingId, UUID customerId);

    /**
     * Get upcoming bookings for manager's hotel
     *
     * @param managerId manager's user ID (from security context)
     * @return List of upcoming bookings for the manager's hotel
     */
    List<BookingResponse> getManagerBookings(UUID managerId);
}
