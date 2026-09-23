package com.apexon.stayease.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.apexon.stayease.enums.BookingStatus;
import com.apexon.stayease.enums.RoomType;
import com.apexon.stayease.model.BookingResponse;
import com.apexon.stayease.model.HotelResponse;
import com.apexon.stayease.model.RoomResponse;
import com.apexon.stayease.security.CustomUserDetails;
import com.apexon.stayease.service.BookingService;
import com.apexon.stayease.service.HotelService;
import com.apexon.stayease.service.RoomService;

@ExtendWith(MockitoExtension.class)
class ManagerControllerTest {

    @Mock private BookingService bookingService;
    @Mock private HotelService hotelService;
    @Mock private RoomService roomService;
    @InjectMocks private ManagerController managerController;

    private CustomUserDetails userDetails;
    private UUID managerId;
    private UUID hotelId;

    @BeforeEach
    void setUp() {
        managerId = UUID.randomUUID();
        hotelId = UUID.randomUUID();
        userDetails = new CustomUserDetails(managerId, "mgr@test.com", "pass", "MANAGER", true);
    }

    @Test
    @DisplayName("Should return manager bookings with 200 OK")
    void testGetManagerBookings() {
        BookingResponse bookingResponse =
                BookingResponse.builder()
                        .bookingId(UUID.randomUUID())
                        .bookingReference("BK-20260901-ABCD1234")
                        .bookingStatus(BookingStatus.CONFIRMED)
                        .checkInDate(LocalDate.now().plusDays(3))
                        .checkOutDate(LocalDate.now().plusDays(7))
                        .totalAmount(BigDecimal.valueOf(600))
                        .roomType(RoomType.SUITE)
                        .build();

        when(bookingService.getManagerBookings(managerId)).thenReturn(List.of(bookingResponse));

        ResponseEntity<List<BookingResponse>> response =
                managerController.getManagerBookings(userDetails);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        verify(bookingService).getManagerBookings(managerId);
    }

    @Test
    @DisplayName("Should return empty list when no bookings")
    void testGetManagerBookings_Empty() {
        when(bookingService.getManagerBookings(managerId)).thenReturn(Collections.emptyList());

        ResponseEntity<List<BookingResponse>> response =
                managerController.getManagerBookings(userDetails);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEmpty();
    }

    @Test
    @DisplayName("Should return hotels managed by current user with 200 OK")
    void testGetMyHotels() {
        HotelResponse hotelResponse =
                HotelResponse.builder()
                        .hotelId(hotelId)
                        .hotelName("Manager Hotel")
                        .cityName("City")
                        .starRating(4)
                        .isActive(true)
                        .build();

        when(hotelService.getHotelsByManagerId(managerId)).thenReturn(List.of(hotelResponse));

        ResponseEntity<List<HotelResponse>> response = managerController.getMyHotels(userDetails);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        verify(hotelService).getHotelsByManagerId(managerId);
    }

    @Test
    @DisplayName("Should return all rooms for a hotel with 200 OK")
    void testGetHotelRooms() {
        RoomResponse roomResponse =
                RoomResponse.builder()
                        .roomId(UUID.randomUUID())
                        .roomNumber("101")
                        .isAvailable(true)
                        .hotelId(hotelId)
                        .hotelName("Manager Hotel")
                        .build();

        when(roomService.getRoomsByHotel(hotelId)).thenReturn(List.of(roomResponse));

        ResponseEntity<List<RoomResponse>> response = managerController.getHotelRooms(hotelId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        verify(roomService).getRoomsByHotel(hotelId);
    }
}
