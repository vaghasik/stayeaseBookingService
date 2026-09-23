package com.apexon.stayease.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.apexon.stayease.entity.Room;
import com.apexon.stayease.enums.RoomType;

/**
 * Room Repository
 * Provides database operations for Room entity.
 *
 */
@Repository
public interface RoomRepository extends JpaRepository<Room, UUID> {

    /**
     * Find rooms by hotel
     *
     * @param hotelId hotel's ID
     * @return List of rooms in the hotel
     */
    @Query("SELECT r FROM Room r WHERE r.hotel.hotelId = :hotelId")
    List<Room> findByHotelId(@Param("hotelId") UUID hotelId);

    /**
     * Find available rooms by hotel
     *
     * @param hotelId hotel's ID
     * @param isAvailable availability status
     * @return List of available rooms
     */
    @Query("SELECT r FROM Room r WHERE r.hotel.hotelId = :hotelId AND r.isAvailable = :isAvailable")
    List<Room> findByHotelIdAndIsAvailable(
            @Param("hotelId") UUID hotelId, @Param("isAvailable") Boolean isAvailable);

    /**
     * Find rooms by room type
     *
     * @param roomType type of room
     * @return List of rooms with specified type
     */
    List<Room> findByRoomType(RoomType roomType);

    /**
     * Find rooms by hotel and room type
     *
     * @param hotelId hotel's ID
     * @param roomType type of room
     * @return List of matching rooms
     */
    @Query("SELECT r FROM Room r WHERE r.hotel.hotelId = :hotelId AND r.roomType = :roomType")
    List<Room> findByHotelIdAndRoomType(
            @Param("hotelId") UUID hotelId, @Param("roomType") RoomType roomType);

    /**
     * Find room by hotel and room number
     *
     * @param hotelId hotel's ID
     * @param roomNumber room number
     * @return Optional containing room if found
     */
    @Query("SELECT r FROM Room r WHERE r.hotel.hotelId = :hotelId AND r.roomNumber = :roomNumber")
    Optional<Room> findByHotelIdAndRoomNumber(
            @Param("hotelId") UUID hotelId, @Param("roomNumber") String roomNumber);

    /**
     * Find rooms within price range
     *
     * @param minPrice minimum price per night
     * @param maxPrice maximum price per night
     * @return List of rooms within price range
     */
    List<Room> findByPricePerNightBetween(BigDecimal minPrice, BigDecimal maxPrice);

    /**
     * Find available rooms by hotel within price range
     *
     * @param hotelId hotel's ID
     * @param minPrice minimum price per night
     * @param maxPrice maximum price per night
     * @param isAvailable availability status
     * @return List of matching rooms
     */
    @Query(
            "SELECT r FROM Room r WHERE r.hotel.hotelId = :hotelId "
                    + "AND r.pricePerNight BETWEEN :minPrice AND :maxPrice "
                    + "AND r.isAvailable = :isAvailable")
    List<Room> findAvailableRoomsByPriceRange(
            @Param("hotelId") UUID hotelId,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            @Param("isAvailable") Boolean isAvailable);

    /**
     * Find rooms by minimum occupancy
     *
     * @param minOccupancy minimum number of guests
     * @return List of rooms that can accommodate the guests
     */
    @Query("SELECT r FROM Room r WHERE r.maxOccupancy >= :minOccupancy")
    List<Room> findByMinOccupancy(@Param("minOccupancy") Integer minOccupancy);

    /**
     * Count rooms by hotel
     *
     * @param hotelId hotel's ID
     * @return Number of rooms in the hotel
     */
    @Query("SELECT COUNT(r) FROM Room r WHERE r.hotel.hotelId = :hotelId")
    Long countByHotelId(@Param("hotelId") UUID hotelId);

    /**
     * Count available rooms by hotel
     *
     * @param hotelId hotel's ID
     * @param isAvailable availability status
     * @return Number of available rooms in the hotel
     */
    @Query(
            "SELECT COUNT(r) FROM Room r WHERE r.hotel.hotelId = :hotelId AND r.isAvailable = :isAvailable")
    Long countAvailableRoomsByHotelId(
            @Param("hotelId") UUID hotelId, @Param("isAvailable") Boolean isAvailable);
}
