package com.apexon.stayease.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

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

import com.apexon.stayease.model.HotelRequest;
import com.apexon.stayease.model.HotelResponse;
import com.apexon.stayease.service.HotelService;

@ExtendWith(MockitoExtension.class)
class HotelControllerTest {

    @Mock private HotelService hotelService;
    @InjectMocks private HotelController hotelController;

    private HotelRequest hotelRequest;
    private HotelResponse hotelResponse;
    private UUID hotelId;

    @BeforeEach
    void setUp() {
        hotelId = UUID.randomUUID();
        hotelRequest =
                HotelRequest.builder()
                        .hotelName("Grand Plaza")
                        .cityName("NYC")
                        .countryName("USA")
                        .starRating(5)
                        .managerId(UUID.randomUUID())
                        .build();
        hotelResponse =
                HotelResponse.builder()
                        .hotelId(hotelId)
                        .hotelName("Grand Plaza")
                        .cityName("NYC")
                        .starRating(5)
                        .isActive(true)
                        .build();
    }

    @Test
    @DisplayName("Should return hotels by city with 200 OK")
    void testGetHotelsByCity() {
        when(hotelService.getHotelsByCity("NYC")).thenReturn(List.of(hotelResponse));

        ResponseEntity<List<HotelResponse>> response = hotelController.getHotelsByCity("NYC");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        verify(hotelService).getHotelsByCity("NYC");
    }

    @Test
    @DisplayName("Should return hotel by ID with 200 OK")
    void testGetHotelById() {
        when(hotelService.getHotelById(hotelId)).thenReturn(hotelResponse);

        ResponseEntity<HotelResponse> response = hotelController.getHotelById(hotelId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getHotelName()).isEqualTo("Grand Plaza");
    }

    @Test
    @DisplayName("Should create hotel with 201 CREATED")
    void testCreateHotel() {
        when(hotelService.createHotel(any())).thenReturn(hotelResponse);

        ResponseEntity<HotelResponse> response = hotelController.createHotel(hotelRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().getHotelId()).isEqualTo(hotelId);
        verify(hotelService).createHotel(any());
    }

    @Test
    @DisplayName("Should update hotel with 200 OK")
    void testUpdateHotel() {
        when(hotelService.updateHotel(eq(hotelId), any())).thenReturn(hotelResponse);

        ResponseEntity<HotelResponse> response = hotelController.updateHotel(hotelId, hotelRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(hotelService).updateHotel(eq(hotelId), any());
    }

    @Test
    @DisplayName("Should delete hotel with 204 NO_CONTENT")
    void testDeleteHotel() {
        doNothing().when(hotelService).deleteHotel(hotelId);

        ResponseEntity<Void> response = hotelController.deleteHotel(hotelId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(hotelService).deleteHotel(hotelId);
    }

    @Test
    @DisplayName("Should return all hotels with 200 OK")
    void testGetAllHotels() {
        when(hotelService.getAllHotels()).thenReturn(List.of(hotelResponse));

        ResponseEntity<List<HotelResponse>> response = hotelController.getAllHotels();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        verify(hotelService).getAllHotels();
    }
}
