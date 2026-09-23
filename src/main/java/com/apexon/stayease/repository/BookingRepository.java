package com.apexon.stayease.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.apexon.stayease.entity.Booking;
import com.apexon.stayease.enums.BookingStatus;

/**
 * Booking Repository
 * Provides database operations for Booking entity.
 *
 */
@Repository
public interface BookingRepository extends JpaRepository<Booking, UUID> {

    /**
     * Find booking by booking reference
     *
     * @param bookingReference unique booking reference
     * @return Optional containing booking if found
     */
    Optional<Booking> findByBookingReference(String bookingReference);

    /**
     * Find bookings by customer
     *
     * @param customerId customer's user ID
     * @return List of bookings made by the customer
     */
    @Query("SELECT b FROM Booking b WHERE b.customer.userId = :customerId")
    List<Booking> findByCustomerId(@Param("customerId") UUID customerId);

    /**
     * Find bookings by customer and status
     *
     * @param customerId customer's user ID
     * @param bookingStatus booking status
     * @return List of matching bookings
     */
    @Query(
            "SELECT b FROM Booking b WHERE b.customer.userId = :customerId AND b.bookingStatus = :bookingStatus")
    List<Booking> findByCustomerIdAndBookingStatus(
            @Param("customerId") UUID customerId,
            @Param("bookingStatus") BookingStatus bookingStatus);

    /**
     * Find bookings by room
     *
     * @param roomId room's ID
     * @return List of bookings for the room
     */
    @Query("SELECT b FROM Booking b WHERE b.room.roomId = :roomId")
    List<Booking> findByRoomId(@Param("roomId") UUID roomId);

    /**
     * Find bookings by hotel (through room relationship)
     *
     * @param hotelId hotel's ID
     * @return List of bookings for the hotel
     */
    @Query("SELECT b FROM Booking b WHERE b.room.hotel.hotelId = :hotelId")
    List<Booking> findByHotelId(@Param("hotelId") UUID hotelId);

    /**
     * Find bookings by status
     *
     * @param bookingStatus booking status
     * @return List of bookings with specified status
     */
    List<Booking> findByBookingStatus(BookingStatus bookingStatus);

    /**
     * Find bookings by check-in date
     *
     * @param checkInDate check-in date
     * @return List of bookings with specified check-in date
     */
    List<Booking> findByCheckInDate(LocalDate checkInDate);

    /**
     * Find bookings by check-in date range
     *
     * @param startDate start of range
     * @param endDate end of range
     * @return List of bookings within the date range
     */
    List<Booking> findByCheckInDateBetween(LocalDate startDate, LocalDate endDate);

    /**
     * Find conflicting bookings for a room Checks if there are any bookings that overlap with the
     * given date range
     *
     * @param roomId room's ID
     * @param checkInDate requested check-in date
     * @param checkOutDate requested check-out date
     * @param excludeStatuses statuses to exclude (typically CANCELLED)
     * @return List of conflicting bookings
     */
    @Query(
            "SELECT b FROM Booking b WHERE b.room.roomId = :roomId "
                    + "AND b.bookingStatus NOT IN :excludeStatuses "
                    + "AND ((b.checkInDate <= :checkOutDate AND b.checkOutDate >= :checkInDate))")
    List<Booking> findConflictingBookings(
            @Param("roomId") UUID roomId,
            @Param("checkInDate") LocalDate checkInDate,
            @Param("checkOutDate") LocalDate checkOutDate,
            @Param("excludeStatuses") List<BookingStatus> excludeStatuses);

    /**
     * Find upcoming bookings for a customer
     *
     * @param customerId customer's user ID
     * @param today current date
     * @return List of upcoming bookings
     */
    @Query(
            "SELECT b FROM Booking b WHERE b.customer.userId = :customerId "
                    + "AND b.checkInDate >= :today "
                    + "AND b.bookingStatus IN ('PENDING', 'CONFIRMED') "
                    + "ORDER BY b.checkInDate ASC")
    List<Booking> findUpcomingBookingsByCustomerId(
            @Param("customerId") UUID customerId, @Param("today") LocalDate today);

    /**
     * Find past bookings for a customer
     *
     * @param customerId customer's user ID
     * @param today current date
     * @return List of past bookings
     */
    @Query(
            "SELECT b FROM Booking b WHERE b.customer.userId = :customerId "
                    + "AND b.checkOutDate < :today "
                    + "ORDER BY b.checkOutDate DESC")
    List<Booking> findPastBookingsByCustomerId(
            @Param("customerId") UUID customerId, @Param("today") LocalDate today);

    /**
     * Count bookings by status
     *
     * @param bookingStatus booking status
     * @return Number of bookings with the status
     */
    Long countByBookingStatus(BookingStatus bookingStatus);

    /**
     * Count bookings by customer
     *
     * @param customerId customer's user ID
     * @return Number of bookings made by the customer
     */
    @Query("SELECT COUNT(b) FROM Booking b WHERE b.customer.userId = :customerId")
    Long countByCustomerId(@Param("customerId") UUID customerId);

    /**
     * Check if booking reference exists
     *
     * @param bookingReference booking reference to check
     * @return true if exists, false otherwise
     */
    boolean existsByBookingReference(String bookingReference);
}
