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

import com.apexon.stayease.enums.RoomType;
import com.apexon.stayease.model.RoomRequest;
import com.apexon.stayease.model.RoomResponse;
import com.apexon.stayease.security.CustomUserDetails;
import com.apexon.stayease.service.RoomService;

@ExtendWith(MockitoExtension.class)
class RoomControllerTest {

    @Mock private RoomService roomService;
    @InjectMocks private RoomController roomController;

    private RoomRequest roomRequest;
    private RoomResponse roomResponse;
    private UUID hotelId;
    private UUID roomId;
    private CustomUserDetails userDetails;

    @BeforeEach
    void setUp() {
        hotelId = UUID.randomUUID();
        roomId = UUID.randomUUID();
        userDetails =
                new CustomUserDetails(
                        UUID.randomUUID(), "manager@stayease.com", "pw", "MANAGER", true);
        roomRequest =
                RoomRequest.builder()
                        .roomNumber("101")
                        .roomType(RoomType.DELUXE)
                        .pricePerNight(BigDecimal.valueOf(200))
                        .maxOccupancy(2)
                        .build();
        roomResponse =
                RoomResponse.builder()
                        .roomId(roomId)
                        .roomNumber("101")
                        .roomType(RoomType.DELUXE)
                        .pricePerNight(BigDecimal.valueOf(200))
                        .maxOccupancy(2)
                        .isAvailable(true)
                        .hotelId(hotelId)
                        .build();
    }

    @Test
    @DisplayName("Should return available rooms with 200 OK")
    void testGetAvailableRooms() {
        LocalDate checkIn = LocalDate.now().plusDays(1);
        LocalDate checkOut = LocalDate.now().plusDays(3);
        when(roomService.getAvailableRooms(hotelId, checkIn, checkOut))
                .thenReturn(List.of(roomResponse));

        ResponseEntity<List<RoomResponse>> response =
                roomController.getAvailableRooms(hotelId, checkIn, checkOut);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
    }

    @Test
    @DisplayName("Should create room with 201 CREATED")
    void testCreateRoom() {
        when(roomService.createRoom(eq(hotelId), any(), any(), any())).thenReturn(roomResponse);

        ResponseEntity<RoomResponse> response =
                roomController.createRoom(hotelId, roomRequest, userDetails);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().getRoomNumber()).isEqualTo("101");
    }

    @Test
    @DisplayName("Should update room with 200 OK")
    void testUpdateRoom() {
        when(roomService.updateRoom(eq(roomId), any(), any(), any())).thenReturn(roomResponse);

        ResponseEntity<RoomResponse> response =
                roomController.updateRoom(roomId, roomRequest, userDetails);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(roomService).updateRoom(eq(roomId), any(), any(), any());
    }

    @Test
    @DisplayName("Should delete room with 204 NO_CONTENT")
    void testDeleteRoom() {
        doNothing().when(roomService).deleteRoom(eq(roomId), any(), any());

        ResponseEntity<Void> response = roomController.deleteRoom(roomId, userDetails);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    @DisplayName("Should toggle room status with 200 OK")
    void testUpdateRoomStatus() {
        when(roomService.updateRoomStatus(eq(roomId), eq(false), any(), any()))
                .thenReturn(roomResponse);

        ResponseEntity<RoomResponse> response =
                roomController.updateRoomStatus(roomId, false, userDetails);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(roomService).updateRoomStatus(eq(roomId), eq(false), any(), any());
    }

    @Test
    @DisplayName("Cover lines 118-127: getRoomById delegates and returns response")
    void testGetRoomById_CoverLines118_127() {
        RoomResponse resp = RoomResponse.builder().roomId(roomId).roomNumber("101").build();
        when(roomService.getRoomById(roomId)).thenReturn(resp);

        ResponseEntity<RoomResponse> response = roomController.getRoomById(roomId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(resp);
        verify(roomService).getRoomById(roomId);
    }
}
