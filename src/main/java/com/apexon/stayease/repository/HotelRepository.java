package com.apexon.stayease.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.apexon.stayease.entity.Hotel;

/**
 * Hotel Repository
 * Provides database operations for Hotel entity.
 *
 */
@Repository
public interface HotelRepository extends JpaRepository<Hotel, UUID> {

    /**
     * Find hotels by city name
     *
     * @param cityName city to search for
     * @return List of hotels in the city
     */
    List<Hotel> findByCityName(String cityName);

    /**
     * Find hotels by city name and active status
     *
     * @param cityName city to search for
     * @param isActive active status
     * @return List of active hotels in the city
     */
    List<Hotel> findByCityNameAndIsActive(String cityName, Boolean isActive);

    /**
     * Find hotels by country name
     *
     * @param countryName country to search for
     * @return List of hotels in the country
     */
    List<Hotel> findByCountryName(String countryName);

    /**
     * Find hotels by star rating
     *
     * @param starRating rating to search for
     * @return List of hotels with the rating
     */
    List<Hotel> findByStarRating(Integer starRating);

    /**
     * Find hotels by star rating greater than or equal to specified value
     *
     * @param starRating minimum star rating
     * @return List of hotels meeting the criteria
     */
    List<Hotel> findByStarRatingGreaterThanEqual(Integer starRating);

    /**
     * Find hotels by manager
     *
     * @param managerId manager's user ID
     * @return List of hotels managed by the user
     */
    @Query("SELECT h FROM Hotel h WHERE h.manager.userId = :managerId")
    List<Hotel> findByManagerId(@Param("managerId") UUID managerId);

    /**
     * Find active hotels in a city
     *
     * @param isActive active status
     * @return List of active hotels
     */
    List<Hotel> findByIsActive(Boolean isActive);

    /**
     * Search hotels by name (case-insensitive)
     *
     * @param hotelName name to search for
     * @return List of matching hotels
     */
    @Query(
            "SELECT h FROM Hotel h WHERE LOWER(h.hotelName) LIKE LOWER(CONCAT('%', :hotelName, '%'))")
    List<Hotel> searchByHotelName(@Param("hotelName") String hotelName);

    /**
     * Find hotels by city and star rating
     *
     * @param cityName city to search for
     * @param starRating minimum star rating
     * @param isActive active status
     * @return List of matching hotels
     */
    @Query(
            "SELECT h FROM Hotel h WHERE h.cityName = :cityName AND h.starRating >= :starRating AND h.isActive = :isActive")
    List<Hotel> findByCityAndRating(
            @Param("cityName") String cityName,
            @Param("starRating") Integer starRating,
            @Param("isActive") Boolean isActive);
}
