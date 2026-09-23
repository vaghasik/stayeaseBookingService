package com.apexon.stayease.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.apexon.stayease.entity.Booking;
import com.apexon.stayease.entity.Hotel;
import com.apexon.stayease.entity.Room;
import com.apexon.stayease.entity.User;
import com.apexon.stayease.enums.BookingStatus;
import com.apexon.stayease.model.RoomRequest;
import com.apexon.stayease.model.RoomResponse;
import com.apexon.stayease.repository.BookingRepository;
import com.apexon.stayease.repository.HotelRepository;
import com.apexon.stayease.repository.RoomRepository;
import com.apexon.stayease.service.impl.RoomServiceImpl;

@ExtendWith(MockitoExtension.class)
class RoomServiceTest {

    @Mock private RoomRepository roomRepository;
    @Mock private HotelRepository hotelRepository;
    @Mock private BookingRepository bookingRepository;

    @InjectMocks private RoomServiceImpl roomService;

    private UUID hotelId;
    private UUID roomId;
    private UUID managerId;

    @BeforeEach
    void setUp() {
        hotelId = UUID.randomUUID();
        roomId = UUID.randomUUID();
        managerId = UUID.randomUUID();
    }

    @Test
    @DisplayName("getAvailableRooms - hotel not found")
    void testGetAvailableRooms_HotelNotFound() {
        when(hotelRepository.findById(hotelId)).thenReturn(Optional.empty());
        assertThatThrownBy(
                        () ->
                                roomService.getAvailableRooms(
                                        hotelId, LocalDate.now(), LocalDate.now().plusDays(1)))
                .isInstanceOf(com.apexon.stayease.exception.ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("getAvailableRooms - null dates")
    void testGetAvailableRooms_NullDates() {
        Hotel h = Hotel.builder().hotelId(hotelId).hotelName("H").build();
        when(hotelRepository.findById(hotelId)).thenReturn(Optional.of(h));
        assertThatThrownBy(
                        () ->
                                roomService.getAvailableRooms(
                                        hotelId, null, LocalDate.now().plusDays(1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Check-in and check-out dates are required");
    }

    @Test
    @DisplayName("getAvailableRooms - invalid date range")
    void testGetAvailableRooms_InvalidRange() {
        Hotel h = Hotel.builder().hotelId(hotelId).hotelName("H").build();
        when(hotelRepository.findById(hotelId)).thenReturn(Optional.of(h));
        LocalDate checkIn = LocalDate.now().plusDays(3);
        LocalDate checkOut = LocalDate.now().plusDays(2);
        assertThatThrownBy(() -> roomService.getAvailableRooms(hotelId, checkIn, checkOut))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Check-out date must be after check-in date");
    }

    @Test
    @DisplayName("getAvailableRooms - filters conflicting bookings")
    void testGetAvailableRooms_FiltersConflicts() {
        Hotel h = Hotel.builder().hotelId(hotelId).hotelName("H").build();
        when(hotelRepository.findById(hotelId)).thenReturn(Optional.of(h));

        Room r1 =
                Room.builder()
                        .roomId(UUID.randomUUID())
                        .roomNumber("101")
                        .isAvailable(true)
                        .hotel(h)
                        .build();
        Room r2 =
                Room.builder()
                        .roomId(UUID.randomUUID())
                        .roomNumber("102")
                        .isAvailable(true)
                        .hotel(h)
                        .build();
        when(roomRepository.findByHotelIdAndIsAvailable(hotelId, true)).thenReturn(List.of(r1, r2));

        // r1 has conflicting booking
        when(bookingRepository.findConflictingBookings(eq(r1.getRoomId()), any(), any(), anyList()))
                .thenReturn(List.of(new Booking()));
        // r2 has no conflicting bookings
        when(bookingRepository.findConflictingBookings(eq(r2.getRoomId()), any(), any(), anyList()))
                .thenReturn(List.of());

        List<RoomResponse> res =
                roomService.getAvailableRooms(
                        hotelId, LocalDate.now().plusDays(1), LocalDate.now().plusDays(3));
        assertThat(res).hasSize(1);
        assertThat(res.get(0).getRoomNumber()).isEqualTo("102");
    }

    @Test
    @DisplayName("getRoomsByHotel - not found")
    void testGetRoomsByHotel_NotFound() {
        when(hotelRepository.findById(hotelId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> roomService.getRoomsByHotel(hotelId))
                .isInstanceOf(com.apexon.stayease.exception.ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("getRoomsByHotel - success")
    void testGetRoomsByHotel_Success() {
        when(hotelRepository.findById(hotelId))
                .thenReturn(Optional.of(Hotel.builder().hotelId(hotelId).hotelName("H").build()));
        Room r =
                Room.builder()
                        .roomId(roomId)
                        .roomNumber("201")
                        .hotel(Hotel.builder().hotelId(hotelId).hotelName("H").build())
                        .build();
        when(roomRepository.findByHotelId(hotelId)).thenReturn(List.of(r));

        List<RoomResponse> res = roomService.getRoomsByHotel(hotelId);
        assertThat(res).hasSize(1);
        assertThat(res.get(0).getRoomNumber()).isEqualTo("201");
    }

    @Test
    @DisplayName("getRoomById - not found")
    void testGetRoomById_NotFound() {
        when(roomRepository.findById(roomId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> roomService.getRoomById(roomId))
                .isInstanceOf(com.apexon.stayease.exception.ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("getRoomById - success")
    void testGetRoomById_Success() {
        Hotel h = Hotel.builder().hotelId(hotelId).hotelName("H").build();
        Room r = Room.builder().roomId(roomId).roomNumber("301").hotel(h).build();
        when(roomRepository.findById(roomId)).thenReturn(Optional.of(r));

        RoomResponse resp = roomService.getRoomById(roomId);
        assertThat(resp.getRoomNumber()).isEqualTo("301");
        assertThat(resp.getHotelId()).isEqualTo(hotelId);
    }

    @Test
    @DisplayName("createRoom - hotel not found")
    void testCreateRoom_HotelNotFound() {
        RoomRequest req = RoomRequest.builder().roomNumber("401").build();
        when(hotelRepository.findById(hotelId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> roomService.createRoom(hotelId, req, managerId, "MANAGER"))
                .isInstanceOf(com.apexon.stayease.exception.ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("createRoom - manager not owner")
    void testCreateRoom_ManagerNotOwner() {
        User mgr = User.builder().userId(UUID.randomUUID()).fullName("Other").build();
        Hotel h = Hotel.builder().hotelId(hotelId).hotelName("H").manager(mgr).build();
        when(hotelRepository.findById(hotelId)).thenReturn(Optional.of(h));

        RoomRequest req = RoomRequest.builder().roomNumber("402").build();

        assertThatThrownBy(() -> roomService.createRoom(hotelId, req, managerId, "MANAGER"))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
    }

    @Test
    @DisplayName("createRoom - room number exists")
    void testCreateRoom_RoomNumberExists() {
        User mgr = User.builder().userId(managerId).fullName("Mgr").build();
        Hotel h = Hotel.builder().hotelId(hotelId).hotelName("H").manager(mgr).build();
        when(hotelRepository.findById(hotelId)).thenReturn(Optional.of(h));

        RoomRequest req = RoomRequest.builder().roomNumber("403").build();
        when(roomRepository.findByHotelIdAndRoomNumber(hotelId, "403"))
                .thenReturn(
                        Optional.of(
                                Room.builder()
                                        .roomId(UUID.randomUUID())
                                        .roomNumber("403")
                                        .hotel(h)
                                        .build()));

        assertThatThrownBy(() -> roomService.createRoom(hotelId, req, managerId, "MANAGER"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    @DisplayName("createRoom - admin can create")
    void testCreateRoom_AdminAllowed() {
        User mgr = User.builder().userId(managerId).fullName("Mgr").build();
        Hotel h = Hotel.builder().hotelId(hotelId).hotelName("H").manager(mgr).build();
        when(hotelRepository.findById(hotelId)).thenReturn(Optional.of(h));

        RoomRequest req =
                RoomRequest.builder()
                        .roomNumber("404")
                        .roomType(null)
                        .pricePerNight(null)
                        .maxOccupancy(2)
                        .build();
        Room saved = Room.builder().roomId(roomId).roomNumber("404").hotel(h).build();
        when(roomRepository.save(any(Room.class))).thenReturn(saved);

        RoomResponse resp = roomService.createRoom(hotelId, req, UUID.randomUUID(), "ADMIN");
        assertThat(resp.getRoomNumber()).isEqualTo("404");
    }

    @Test
    @DisplayName("updateRoom - room not found")
    void testUpdateRoom_NotFound() {
        RoomRequest req = RoomRequest.builder().roomNumber("501").build();
        when(roomRepository.findById(roomId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> roomService.updateRoom(roomId, req, managerId, "MANAGER"))
                .isInstanceOf(com.apexon.stayease.exception.ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("updateRoom - manager not owner")
    void testUpdateRoom_ManagerNotOwner() {
        User mgr = User.builder().userId(UUID.randomUUID()).fullName("Other").build();
        Hotel h = Hotel.builder().hotelId(hotelId).hotelName("H").manager(mgr).build();
        Room existing = Room.builder().roomId(roomId).roomNumber("501").hotel(h).build();
        when(roomRepository.findById(roomId)).thenReturn(Optional.of(existing));

        RoomRequest req = RoomRequest.builder().roomNumber("502").build();
        assertThatThrownBy(() -> roomService.updateRoom(roomId, req, managerId, "MANAGER"))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
    }

    @Test
    @DisplayName("updateRoom - room number conflict")
    void testUpdateRoom_NumberConflict() {
        User mgr = User.builder().userId(managerId).fullName("Mgr").build();
        Hotel h = Hotel.builder().hotelId(hotelId).hotelName("H").manager(mgr).build();
        Room existing = Room.builder().roomId(roomId).roomNumber("501").hotel(h).build();
        when(roomRepository.findById(roomId)).thenReturn(Optional.of(existing));

        RoomRequest req = RoomRequest.builder().roomNumber("502").build();
        when(roomRepository.findByHotelIdAndRoomNumber(hotelId, "502"))
                .thenReturn(
                        Optional.of(
                                Room.builder()
                                        .roomId(UUID.randomUUID())
                                        .roomNumber("502")
                                        .hotel(h)
                                        .build()));

        assertThatThrownBy(() -> roomService.updateRoom(roomId, req, managerId, "MANAGER"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    @DisplayName("updateRoom - success")
    void testUpdateRoom_Success() {
        User mgr = User.builder().userId(managerId).fullName("Mgr").build();
        Hotel h = Hotel.builder().hotelId(hotelId).hotelName("H").manager(mgr).build();
        Room existing =
                Room.builder().roomId(roomId).roomNumber("601").hotel(h).isAvailable(true).build();
        when(roomRepository.findById(roomId)).thenReturn(Optional.of(existing));

        RoomRequest req =
                RoomRequest.builder()
                        .roomNumber("601")
                        .roomDescription("New desc")
                        .maxOccupancy(3)
                        .isAvailable(false)
                        .build();
        when(roomRepository.save(existing)).thenReturn(existing);

        RoomResponse resp = roomService.updateRoom(roomId, req, managerId, "MANAGER");
        assertThat(resp.getRoomNumber()).isEqualTo("601");
        assertThat(resp.getMaxOccupancy()).isEqualTo(3);
    }

    @Test
    @DisplayName("deleteRoom - room not found")
    void testDeleteRoom_NotFound() {
        when(roomRepository.findById(roomId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> roomService.deleteRoom(roomId, managerId, "MANAGER"))
                .isInstanceOf(com.apexon.stayease.exception.ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("deleteRoom - has active bookings")
    void testDeleteRoom_HasActiveBookings() {
        User mgr = User.builder().userId(managerId).fullName("Mgr").build();
        Hotel h = Hotel.builder().hotelId(hotelId).hotelName("H").manager(mgr).build();
        Room existing = Room.builder().roomId(roomId).roomNumber("701").hotel(h).build();
        when(roomRepository.findById(roomId)).thenReturn(Optional.of(existing));

        Booking active =
                Booking.builder()
                        .bookingId(UUID.randomUUID())
                        .bookingStatus(BookingStatus.CONFIRMED)
                        .checkOutDate(LocalDate.now().plusDays(1))
                        .build();
        when(bookingRepository.findByRoomId(roomId)).thenReturn(List.of(active));

        assertThatThrownBy(() -> roomService.deleteRoom(roomId, managerId, "MANAGER"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot delete room with active or future bookings");
    }

    @Test
    @DisplayName("deleteRoom - success")
    void testDeleteRoom_Success() {
        User mgr = User.builder().userId(managerId).fullName("Mgr").build();
        Hotel h = Hotel.builder().hotelId(hotelId).hotelName("H").manager(mgr).build();
        Room existing = Room.builder().roomId(roomId).roomNumber("702").hotel(h).build();
        when(roomRepository.findById(roomId)).thenReturn(Optional.of(existing));

        when(bookingRepository.findByRoomId(roomId)).thenReturn(List.of());

        roomService.deleteRoom(roomId, managerId, "MANAGER");
        verify(roomRepository).delete(existing);
    }

    @Test
    @DisplayName("updateRoomStatus - not found")
    void testUpdateRoomStatus_NotFound() {
        when(roomRepository.findById(roomId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> roomService.updateRoomStatus(roomId, false, managerId, "MANAGER"))
                .isInstanceOf(com.apexon.stayease.exception.ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("updateRoomStatus - manager not owner")
    void testUpdateRoomStatus_ManagerNotOwner() {
        User mgr = User.builder().userId(UUID.randomUUID()).fullName("Other").build();
        Hotel h = Hotel.builder().hotelId(hotelId).hotelName("H").manager(mgr).build();
        Room existing = Room.builder().roomId(roomId).roomNumber("801").hotel(h).build();
        when(roomRepository.findById(roomId)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> roomService.updateRoomStatus(roomId, false, managerId, "MANAGER"))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
    }

    @Test
    @DisplayName("updateRoomStatus - success")
    void testUpdateRoomStatus_Success() {
        User mgr = User.builder().userId(managerId).fullName("Mgr").build();
        Hotel h = Hotel.builder().hotelId(hotelId).hotelName("H").manager(mgr).build();
        Room existing =
                Room.builder().roomId(roomId).roomNumber("802").hotel(h).isAvailable(true).build();
        when(roomRepository.findById(roomId)).thenReturn(Optional.of(existing));
        when(roomRepository.save(existing)).thenReturn(existing);

        RoomResponse resp = roomService.updateRoomStatus(roomId, false, managerId, "MANAGER");
        assertThat(resp.getIsAvailable()).isFalse();
    }
}
