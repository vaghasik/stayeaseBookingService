package com.apexon.stayease.service.impl;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.apexon.stayease.entity.Hotel;
import com.apexon.stayease.entity.User;
import com.apexon.stayease.enums.UserRole;
import com.apexon.stayease.exception.ResourceNotFoundException;
import com.apexon.stayease.model.HotelRequest;
import com.apexon.stayease.model.HotelResponse;
import com.apexon.stayease.repository.HotelRepository;
import com.apexon.stayease.repository.UserRepository;
import com.apexon.stayease.service.HotelService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Hotel Service Implementation
 * Implements business logic for Hotel management.
 *
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class HotelServiceImpl implements HotelService {

    private final HotelRepository hotelRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public List<HotelResponse> getHotelsByCity(String city) {
        log.info("Fetching hotels for city: {}", city);
        List<Hotel> hotels = hotelRepository.findByCityNameAndIsActive(city, true);
        return hotels.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<HotelResponse> getHotelsByManagerId(UUID managerId) {
        log.info("Fetching hotels for manager: {}", managerId);
        List<Hotel> hotels = hotelRepository.findByManagerId(managerId);
        return hotels.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<HotelResponse> getAllHotels() {
        log.info("Fetching all hotels");
        List<Hotel> hotels = hotelRepository.findAll();
        return hotels.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public HotelResponse getHotelById(UUID hotelId) {
        log.info("Fetching hotel with ID: {}", hotelId);
        Hotel hotel =
                hotelRepository
                        .findById(hotelId)
                        .orElseThrow(
                                () -> new ResourceNotFoundException("Hotel", "hotelId", hotelId));
        return mapToResponse(hotel);
    }

    @Override
    @Transactional
    public HotelResponse createHotel(HotelRequest hotelRequest) {
        log.info("Creating new hotel: {}", hotelRequest.getHotelName());

        // Validate manager exists and has MANAGER or ADMIN role
        User manager =
                userRepository
                        .findById(hotelRequest.getManagerId())
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "User", "userId", hotelRequest.getManagerId()));

        if (manager.getUserRole() != UserRole.MANAGER && manager.getUserRole() != UserRole.ADMIN) {
            throw new IllegalArgumentException(
                    "User must have MANAGER or ADMIN role to manage a hotel");
        }

        Hotel hotel =
                Hotel.builder()
                        .hotelName(hotelRequest.getHotelName())
                        .description(hotelRequest.getDescription())
                        .streetAddress(hotelRequest.getStreetAddress())
                        .cityName(hotelRequest.getCityName())
                        .stateName(hotelRequest.getStateName())
                        .countryName(hotelRequest.getCountryName())
                        .postalCode(hotelRequest.getPostalCode())
                        .contactPhone(hotelRequest.getContactPhone())
                        .contactEmail(hotelRequest.getContactEmail())
                        .starRating(hotelRequest.getStarRating())
                        .coverImageUrl(hotelRequest.getCoverImageUrl())
                        .isActive(
                                hotelRequest.getIsActive() != null
                                        ? hotelRequest.getIsActive()
                                        : true)
                        .manager(manager)
                        .build();

        Hotel savedHotel = hotelRepository.save(hotel);
        log.info("Hotel created successfully with ID: {}", savedHotel.getHotelId());
        return mapToResponse(savedHotel);
    }

    @Override
    @Transactional
    public HotelResponse updateHotel(UUID hotelId, HotelRequest hotelRequest) {
        log.info("Updating hotel with ID: {}", hotelId);

        Hotel hotel =
                hotelRepository
                        .findById(hotelId)
                        .orElseThrow(
                                () -> new ResourceNotFoundException("Hotel", "hotelId", hotelId));

        // Validate manager if being updated
        if (hotelRequest.getManagerId() != null
                && !hotelRequest.getManagerId().equals(hotel.getManager().getUserId())) {
            User newManager =
                    userRepository
                            .findById(hotelRequest.getManagerId())
                            .orElseThrow(
                                    () ->
                                            new ResourceNotFoundException(
                                                    "User", "userId", hotelRequest.getManagerId()));

            if (newManager.getUserRole() != UserRole.MANAGER
                    && newManager.getUserRole() != UserRole.ADMIN) {
                throw new IllegalArgumentException(
                        "User must have MANAGER or ADMIN role to manage a hotel");
            }
            hotel.setManager(newManager);
        }

        // Update fields
        hotel.setHotelName(hotelRequest.getHotelName());
        hotel.setDescription(hotelRequest.getDescription());
        hotel.setStreetAddress(hotelRequest.getStreetAddress());
        hotel.setCityName(hotelRequest.getCityName());
        hotel.setStateName(hotelRequest.getStateName());
        hotel.setCountryName(hotelRequest.getCountryName());
        hotel.setPostalCode(hotelRequest.getPostalCode());
        hotel.setContactPhone(hotelRequest.getContactPhone());
        hotel.setContactEmail(hotelRequest.getContactEmail());
        hotel.setStarRating(hotelRequest.getStarRating());
        hotel.setCoverImageUrl(hotelRequest.getCoverImageUrl());

        if (hotelRequest.getIsActive() != null) {
            hotel.setIsActive(hotelRequest.getIsActive());
        }

        Hotel updatedHotel = hotelRepository.save(hotel);
        log.info("Hotel updated successfully with ID: {}", updatedHotel.getHotelId());
        return mapToResponse(updatedHotel);
    }

    @Override
    @Transactional
    public void deleteHotel(UUID hotelId) {
        log.info("Deleting hotel with ID: {}", hotelId);

        Hotel hotel =
                hotelRepository
                        .findById(hotelId)
                        .orElseThrow(
                                () -> new ResourceNotFoundException("Hotel", "hotelId", hotelId));

        hotelRepository.delete(hotel);
        log.info("Hotel deleted successfully with ID: {}", hotelId);
    }

    /**
     * Maps Hotel entity to HotelResponse DTO
     *
     * @param hotel Hotel entity
     * @return HotelResponse DTO
     */
    private HotelResponse mapToResponse(Hotel hotel) {
        return HotelResponse.builder()
                .hotelId(hotel.getHotelId())
                .hotelName(hotel.getHotelName())
                .description(hotel.getDescription())
                .streetAddress(hotel.getStreetAddress())
                .cityName(hotel.getCityName())
                .stateName(hotel.getStateName())
                .countryName(hotel.getCountryName())
                .postalCode(hotel.getPostalCode())
                .contactPhone(hotel.getContactPhone())
                .contactEmail(hotel.getContactEmail())
                .starRating(hotel.getStarRating())
                .coverImageUrl(hotel.getCoverImageUrl())
                .isActive(hotel.getIsActive())
                .managerId(hotel.getManager().getUserId())
                .managerName(hotel.getManager().getFullName())
                .createdAt(hotel.getCreatedAt())
                .updatedAt(hotel.getUpdatedAt())
                .build();
    }
}
