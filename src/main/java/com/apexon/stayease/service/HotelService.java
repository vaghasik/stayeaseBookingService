package com.apexon.stayease.service;

import java.util.List;
import java.util.UUID;

import com.apexon.stayease.model.HotelRequest;
import com.apexon.stayease.model.HotelResponse;

/**
 * Hotel Service Interface
 * Defines business operations for Hotel management.
 *
 */
public interface HotelService {

    /**
     * Get all hotels in a city
     *
     * @param city city name
     * @return List of hotels in the city
     */
    List<HotelResponse> getHotelsByCity(String city);

    /**
     * Get all hotels managed by a specific manager
     *
     * @param managerId manager's unique identifier
     * @return List of hotels managed by the user
     */
    List<HotelResponse> getHotelsByManagerId(UUID managerId);

    /**
     * Get every hotel in the system, regardless of city. Used by the ADMIN hotel directory,
     * since GET /api/hotels always requires a city filter.
     *
     * @return List of all hotels
     */
    List<HotelResponse> getAllHotels();

    /**
     * Get hotel by ID
     *
     * @param hotelId hotel's unique identifier
     * @return Hotel details
     */
    HotelResponse getHotelById(UUID hotelId);

    /**
     * Create a new hotel
     *
     * @param hotelRequest hotel creation request
     * @return Created hotel details
     */
    HotelResponse createHotel(HotelRequest hotelRequest);

    /**
     * Update an existing hotel
     *
     * @param hotelId hotel's unique identifier
     * @param hotelRequest hotel update request
     * @return Updated hotel details
     */
    HotelResponse updateHotel(UUID hotelId, HotelRequest hotelRequest);

    /**
     * Delete a hotel
     *
     * @param hotelId hotel's unique identifier
     */
    void deleteHotel(UUID hotelId);
}
