package com.apexon.stayease.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

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
import com.apexon.stayease.model.BookingRequest;
import com.apexon.stayease.model.BookingResponse;
import com.apexon.stayease.security.CustomUserDetails;
import com.apexon.stayease.service.BookingService;

@ExtendWith(MockitoExtension.class)
class BookingControllerTest {

    @Mock private BookingService bookingService;
    @InjectMocks private BookingController bookingController;

    private CustomUserDetails userDetails;
    private BookingRequest bookingRequest;
    private BookingResponse bookingResponse;
    private UUID customerId;
    private UUID bookingId;

    @BeforeEach
    void setUp() {
        customerId = UUID.randomUUID();
        bookingId = UUID.randomUUID();

        userDetails = new CustomUserDetails(customerId, "guest@test.com", "pass", "GUEST", true);

        bookingRequest =
                BookingRequest.builder()
                        .roomId(UUID.randomUUID())
                        .checkInDate(LocalDate.now().plusDays(5))
                        .checkOutDate(LocalDate.now().plusDays(9))
                        .numberOfGuests(2)
                        .build();

        bookingResponse =
                BookingResponse.builder()
                        .bookingId(bookingId)
                        .bookingReference("BK-20260825-ABCD1234")
                        .checkInDate(LocalDate.now().plusDays(5))
                        .checkOutDate(LocalDate.now().plusDays(9))
                        .totalAmount(BigDecimal.valueOf(600))
                        .bookingStatus(BookingStatus.CONFIRMED)
                        .customerId(customerId)
                        .customerName("John Guest")
                        .roomType(RoomType.DELUXE)
                        .build();
    }

    @Test
    @DisplayName("Should create booking with 201 CREATED")
    void testCreateBooking() {
        when(bookingService.createBooking(any(), eq(customerId))).thenReturn(bookingResponse);

        ResponseEntity<BookingResponse> response =
                bookingController.createBooking(bookingRequest, userDetails);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().getBookingReference()).isEqualTo("BK-20260825-ABCD1234");
        verify(bookingService).createBooking(any(), eq(customerId));
    }

    @Test
    @DisplayName("Should return my bookings with 200 OK")
    void testGetMyBookings() {
        when(bookingService.getMyBookings(customerId)).thenReturn(List.of(bookingResponse));

        ResponseEntity<List<BookingResponse>> response =
                bookingController.getMyBookings(userDetails);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        verify(bookingService).getMyBookings(customerId);
    }

    @Test
    @DisplayName("Should cancel booking with 200 OK")
    void testCancelBooking() {
        BookingResponse cancelled =
                BookingResponse.builder()
                        .bookingId(bookingId)
                        .bookingStatus(BookingStatus.CANCELLED)
                        .build();
        when(bookingService.cancelBooking(bookingId, customerId)).thenReturn(cancelled);

        ResponseEntity<BookingResponse> response =
                bookingController.cancelBooking(bookingId, userDetails);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getBookingStatus()).isEqualTo(BookingStatus.CANCELLED);
    }
}
