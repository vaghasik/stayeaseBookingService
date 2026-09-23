package com.apexon.stayease.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.apexon.stayease.entity.HotelAmenity;

/**
 * HotelAmenity Repository
 * Provides database operations for HotelAmenity entity.
 *
 */
@Repository
public interface HotelAmenityRepository
        extends JpaRepository<HotelAmenity, HotelAmenity.HotelAmenityId> {

    /**
     * Find all amenities for a hotel
     *
     * @param hotelId hotel's ID
     * @return List of amenities for the hotel
     */
    List<HotelAmenity> findByHotelId(UUID hotelId);

    /**
     * Find hotels that have a specific amenity
     *
     * @param amenityName amenity to search for
     * @return List of hotel amenities with the specified amenity
     */
    List<HotelAmenity> findByAmenityName(String amenityName);

    /**
     * Check if a hotel has a specific amenity
     *
     * @param hotelId hotel's ID
     * @param amenityName amenity name
     * @return true if hotel has the amenity, false otherwise
     */
    boolean existsByHotelIdAndAmenityName(UUID hotelId, String amenityName);

    /**
     * Delete all amenities for a hotel
     *
     * @param hotelId hotel's ID
     * @return Number of amenities deleted
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM HotelAmenity ha WHERE ha.hotelId = :hotelId")
    int deleteByHotelId(@Param("hotelId") UUID hotelId);

    /**
     * Delete a specific amenity from a hotel
     *
     * @param hotelId hotel's ID
     * @param amenityName amenity name
     * @return Number of amenities deleted (0 or 1)
     */
    @Modifying
    @Transactional
    @Query(
            "DELETE FROM HotelAmenity ha WHERE ha.hotelId = :hotelId AND ha.amenityName = :amenityName")
    int deleteByHotelIdAndAmenityName(
            @Param("hotelId") UUID hotelId, @Param("amenityName") String amenityName);

    /**
     * Count amenities for a hotel
     *
     * @param hotelId hotel's ID
     * @return Number of amenities the hotel has
     */
    Long countByHotelId(UUID hotelId);

    /**
     * Find hotels that have multiple specified amenities (case-insensitive)
     *
     * @param amenityNames list of amenity names to search for
     * @param minCount minimum number of amenities that must match
     * @return List of hotel IDs that have at least minCount of the specified amenities
     */
    @Query(
            "SELECT ha.hotelId FROM HotelAmenity ha "
                    + "WHERE LOWER(ha.amenityName) IN :amenityNames "
                    + "GROUP BY ha.hotelId "
                    + "HAVING COUNT(DISTINCT ha.amenityName) >= :minCount")
    List<UUID> findHotelsByAmenities(
            @Param("amenityNames") List<String> amenityNames, @Param("minCount") Long minCount);

    /**
     * Get distinct amenity names across all hotels
     *
     * @return List of unique amenity names
     */
    @Query("SELECT DISTINCT ha.amenityName FROM HotelAmenity ha ORDER BY ha.amenityName")
    List<String> findDistinctAmenityNames();
}
