package com.apexon.stayease.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
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

import com.apexon.stayease.entity.*;
import com.apexon.stayease.enums.BookingStatus;
import com.apexon.stayease.model.BookingRequest;
import com.apexon.stayease.model.BookingResponse;
import com.apexon.stayease.repository.BookingRepository;
import com.apexon.stayease.repository.HotelRepository;
import com.apexon.stayease.repository.RoomRepository;
import com.apexon.stayease.repository.UserRepository;
import com.apexon.stayease.service.impl.BookingServiceImpl;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock private BookingRepository bookingRepository;
    @Mock private RoomRepository roomRepository;
    @Mock private UserRepository userRepository;
    @Mock private HotelRepository hotelRepository;

    @InjectMocks private BookingServiceImpl bookingService;

    private UUID customerId;
    private UUID roomId;
    private UUID bookingId;

    @BeforeEach
    void setUp() {
        customerId = UUID.randomUUID();
        roomId = UUID.randomUUID();
        bookingId = UUID.randomUUID();
    }

    @Test
    @DisplayName("createBooking - success path")
    void testCreateBooking_Success() {
        BookingRequest req =
                BookingRequest.builder()
                        .roomId(roomId)
                        .checkInDate(LocalDate.now().plusDays(1))
                        .checkOutDate(LocalDate.now().plusDays(3))
                        .numberOfGuests(2)
                        .specialRequests("None")
                        .build();

        User user =
                User.builder().userId(customerId).fullName("John").emailAddress("a@b.com").build();
        Hotel hotel = Hotel.builder().hotelId(UUID.randomUUID()).hotelName("H").build();
        Room room =
                Room.builder()
                        .roomId(roomId)
                        .isAvailable(true)
                        .maxOccupancy(2)
                        .pricePerNight(BigDecimal.valueOf(100))
                        .hotel(hotel)
                        .roomNumber("101")
                        .build();

        when(userRepository.findById(customerId)).thenReturn(Optional.of(user));
        when(roomRepository.findById(roomId)).thenReturn(Optional.of(room));
        when(bookingRepository.findConflictingBookings(eq(roomId), any(), any(), anyList()))
                .thenReturn(List.of());
        when(bookingRepository.existsByBookingReference(anyString())).thenReturn(false);

        Booking saved =
                Booking.builder()
                        .bookingId(bookingId)
                        .bookingReference("BK-1")
                        .checkInDate(req.getCheckInDate())
                        .checkOutDate(req.getCheckOutDate())
                        .numberOfGuests(req.getNumberOfGuests())
                        .totalAmount(BigDecimal.valueOf(200))
                        .bookingStatus(BookingStatus.CONFIRMED)
                        .specialRequests(req.getSpecialRequests())
                        .customer(user)
                        .room(room)
                        .build();

        when(bookingRepository.save(any(Booking.class))).thenReturn(saved);

        BookingResponse resp = bookingService.createBooking(req, customerId);

        assertThat(resp).isNotNull();
        assertThat(resp.getBookingReference()).isEqualTo("BK-1");
        assertThat(resp.getTotalAmount()).isEqualByComparingTo(BigDecimal.valueOf(200));
        verify(bookingRepository).save(any(Booking.class));
    }

    @Test
    @DisplayName("createBooking - user not found")
    void testCreateBooking_UserNotFound() {
        BookingRequest req =
                BookingRequest.builder()
                        .roomId(roomId)
                        .checkInDate(LocalDate.now().plusDays(1))
                        .checkOutDate(LocalDate.now().plusDays(2))
                        .numberOfGuests(1)
                        .build();
        when(userRepository.findById(customerId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.createBooking(req, customerId))
                .isInstanceOf(com.apexon.stayease.exception.ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("createBooking - room not found")
    void testCreateBooking_RoomNotFound() {
        BookingRequest req =
                BookingRequest.builder()
                        .roomId(roomId)
                        .checkInDate(LocalDate.now().plusDays(1))
                        .checkOutDate(LocalDate.now().plusDays(2))
                        .numberOfGuests(1)
                        .build();
        when(userRepository.findById(customerId)).thenReturn(Optional.of(new User()));
        when(roomRepository.findById(roomId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.createBooking(req, customerId))
                .isInstanceOf(com.apexon.stayease.exception.ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("createBooking - room not available")
    void testCreateBooking_RoomNotAvailable() {
        BookingRequest req =
                BookingRequest.builder()
                        .roomId(roomId)
                        .checkInDate(LocalDate.now().plusDays(1))
                        .checkOutDate(LocalDate.now().plusDays(2))
                        .numberOfGuests(1)
                        .build();
        when(userRepository.findById(customerId)).thenReturn(Optional.of(new User()));
        Room room =
                Room.builder()
                        .roomId(roomId)
                        .isAvailable(false)
                        .maxOccupancy(2)
                        .pricePerNight(BigDecimal.valueOf(100))
                        .build();
        when(roomRepository.findById(roomId)).thenReturn(Optional.of(room));

        assertThatThrownBy(() -> bookingService.createBooking(req, customerId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not available");
    }

    @Test
    @DisplayName("createBooking - invalid dates (past check-in)")
    void testCreateBooking_InvalidDates_PastCheckIn() {
        BookingRequest req =
                BookingRequest.builder()
                        .roomId(roomId)
                        .checkInDate(LocalDate.now().minusDays(1))
                        .checkOutDate(LocalDate.now().plusDays(1))
                        .numberOfGuests(1)
                        .build();
        when(userRepository.findById(customerId)).thenReturn(Optional.of(new User()));
        Room room =
                Room.builder()
                        .roomId(roomId)
                        .isAvailable(true)
                        .maxOccupancy(2)
                        .pricePerNight(BigDecimal.valueOf(100))
                        .build();
        when(roomRepository.findById(roomId)).thenReturn(Optional.of(room));

        assertThatThrownBy(() -> bookingService.createBooking(req, customerId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Check-in date cannot be in the past");
    }

    @Test
    @DisplayName("createBooking - number of guests exceeds max occupancy")
    void testCreateBooking_GuestsExceed() {
        BookingRequest req =
                BookingRequest.builder()
                        .roomId(roomId)
                        .checkInDate(LocalDate.now().plusDays(1))
                        .checkOutDate(LocalDate.now().plusDays(2))
                        .numberOfGuests(3)
                        .build();
        when(userRepository.findById(customerId)).thenReturn(Optional.of(new User()));
        Room room =
                Room.builder()
                        .roomId(roomId)
                        .isAvailable(true)
                        .maxOccupancy(2)
                        .pricePerNight(BigDecimal.valueOf(100))
                        .build();
        when(roomRepository.findById(roomId)).thenReturn(Optional.of(room));

        assertThatThrownBy(() -> bookingService.createBooking(req, customerId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("exceeds room's maximum occupancy");
    }

    @Test
    @DisplayName("createBooking - conflicting bookings")
    void testCreateBooking_Conflicting() {
        BookingRequest req =
                BookingRequest.builder()
                        .roomId(roomId)
                        .checkInDate(LocalDate.now().plusDays(1))
                        .checkOutDate(LocalDate.now().plusDays(2))
                        .numberOfGuests(1)
                        .build();
        when(userRepository.findById(customerId)).thenReturn(Optional.of(new User()));
        Room room =
                Room.builder()
                        .roomId(roomId)
                        .isAvailable(true)
                        .maxOccupancy(2)
                        .pricePerNight(BigDecimal.valueOf(100))
                        .build();
        when(roomRepository.findById(roomId)).thenReturn(Optional.of(room));

        when(bookingRepository.findConflictingBookings(eq(roomId), any(), any(), anyList()))
                .thenReturn(List.of(new Booking()));

        assertThatThrownBy(() -> bookingService.createBooking(req, customerId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not available for the selected dates");
    }

    @Test
    @DisplayName("getMyBookings - success")
    void testGetMyBookings_Success() {
        User user =
                User.builder().userId(customerId).fullName("Jane").emailAddress("j@e.com").build();
        when(userRepository.findById(customerId)).thenReturn(Optional.of(user));

        Hotel hotel = Hotel.builder().hotelId(UUID.randomUUID()).hotelName("H").build();
        Room room = Room.builder().roomId(UUID.randomUUID()).roomNumber("101").hotel(hotel).build();
        Booking booking =
                Booking.builder()
                        .bookingId(bookingId)
                        .bookingReference("BK")
                        .checkInDate(LocalDate.now().plusDays(1))
                        .checkOutDate(LocalDate.now().plusDays(2))
                        .numberOfGuests(1)
                        .totalAmount(BigDecimal.valueOf(100))
                        .bookingStatus(BookingStatus.CONFIRMED)
                        .customer(user)
                        .room(room)
                        .build();

        when(bookingRepository.findByCustomerId(customerId)).thenReturn(List.of(booking));

        List<BookingResponse> res = bookingService.getMyBookings(customerId);
        assertThat(res).hasSize(1);
        assertThat(res.get(0).getBookingReference()).isEqualTo("BK");
    }

    @Test
    @DisplayName("getMyBookings - user not found")
    void testGetMyBookings_UserNotFound() {
        when(userRepository.findById(customerId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> bookingService.getMyBookings(customerId))
                .isInstanceOf(com.apexon.stayease.exception.ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("cancelBooking - success")
    void testCancelBooking_Success() {
        User user =
                User.builder()
                        .userId(customerId)
                        .fullName("John")
                        .emailAddress("john@example.com")
                        .build();
        Hotel hotel = Hotel.builder().hotelId(UUID.randomUUID()).hotelName("HotelX").build();
        Room room = Room.builder().roomId(UUID.randomUUID()).hotel(hotel).build();
        Booking booking =
                Booking.builder()
                        .bookingId(bookingId)
                        .bookingReference("BK")
                        .checkInDate(LocalDate.now().plusDays(2))
                        .checkOutDate(LocalDate.now().plusDays(4))
                        .bookingStatus(BookingStatus.CONFIRMED)
                        .customer(user)
                        .room(room)
                        .build();

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        BookingResponse resp = bookingService.cancelBooking(bookingId, customerId);
        assertThat(resp.getBookingStatus()).isEqualTo(BookingStatus.CANCELLED);
        verify(bookingRepository).save(any(Booking.class));
    }

    @Test
    @DisplayName("cancelBooking - booking not found")
    void testCancelBooking_NotFound() {
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> bookingService.cancelBooking(bookingId, customerId))
                .isInstanceOf(com.apexon.stayease.exception.ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("cancelBooking - not owner")
    void testCancelBooking_NotOwner() {
        User owner = User.builder().userId(UUID.randomUUID()).build();
        Room room = Room.builder().roomId(UUID.randomUUID()).build();
        Booking booking =
                Booking.builder()
                        .bookingId(bookingId)
                        .customer(owner)
                        .bookingStatus(BookingStatus.CONFIRMED)
                        .checkInDate(LocalDate.now().plusDays(2))
                        .build();
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> bookingService.cancelBooking(bookingId, customerId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not authorized");
    }

    @Test
    @DisplayName("cancelBooking - already cancelled")
    void testCancelBooking_AlreadyCancelled() {
        User owner = User.builder().userId(customerId).build();
        Booking booking =
                Booking.builder()
                        .bookingId(bookingId)
                        .customer(owner)
                        .bookingStatus(BookingStatus.CANCELLED)
                        .checkInDate(LocalDate.now().plusDays(2))
                        .build();
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> bookingService.cancelBooking(bookingId, customerId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already cancelled");
    }

    @Test
    @DisplayName("cancelBooking - completed")
    void testCancelBooking_Completed() {
        User owner = User.builder().userId(customerId).build();
        Booking booking =
                Booking.builder()
                        .bookingId(bookingId)
                        .customer(owner)
                        .bookingStatus(BookingStatus.COMPLETED)
                        .checkInDate(LocalDate.now().plusDays(2))
                        .build();
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> bookingService.cancelBooking(bookingId, customerId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot cancel a completed booking");
    }

    @Test
    @DisplayName("cancelBooking - past check-in")
    void testCancelBooking_PastCheckIn() {
        User owner = User.builder().userId(customerId).build();
        Booking booking =
                Booking.builder()
                        .bookingId(bookingId)
                        .customer(owner)
                        .bookingStatus(BookingStatus.CONFIRMED)
                        .checkInDate(LocalDate.now().minusDays(1))
                        .build();
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> bookingService.cancelBooking(bookingId, customerId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("past check-in date");
    }

    @Test
    @DisplayName("getManagerBookings - manager not found")
    void testGetManagerBookings_ManagerNotFound() {
        UUID managerId = UUID.randomUUID();
        when(userRepository.findById(managerId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> bookingService.getManagerBookings(managerId))
                .isInstanceOf(com.apexon.stayease.exception.ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("getManagerBookings - no hotels assigned")
    void testGetManagerBookings_NoHotels() {
        UUID managerId = UUID.randomUUID();
        User manager = User.builder().userId(managerId).build();
        when(userRepository.findById(managerId)).thenReturn(Optional.of(manager));
        when(hotelRepository.findByManagerId(managerId)).thenReturn(List.of());

        List<com.apexon.stayease.model.BookingResponse> res =
                bookingService.getManagerBookings(managerId);
        assertThat(res).isEmpty();
    }

    @Test
    @DisplayName("getManagerBookings - returns upcoming confirmed/pending bookings sorted")
    void testGetManagerBookings_WithBookings() {
        UUID managerId = UUID.randomUUID();
        User manager = User.builder().userId(managerId).build();
        when(userRepository.findById(managerId)).thenReturn(Optional.of(manager));

        Hotel h1 = Hotel.builder().hotelId(UUID.randomUUID()).hotelName("H1").build();
        when(hotelRepository.findByManagerId(managerId)).thenReturn(List.of(h1));

        User cust1 =
                User.builder()
                        .userId(UUID.randomUUID())
                        .fullName("Cust1")
                        .emailAddress("c1@x.com")
                        .build();
        User cust2 =
                User.builder()
                        .userId(UUID.randomUUID())
                        .fullName("Cust2")
                        .emailAddress("c2@x.com")
                        .build();
        User cust3 =
                User.builder()
                        .userId(UUID.randomUUID())
                        .fullName("Cust3")
                        .emailAddress("c3@x.com")
                        .build();

        Booking b1 =
                Booking.builder()
                        .bookingId(UUID.randomUUID())
                        .checkInDate(LocalDate.now().plusDays(5))
                        .bookingStatus(BookingStatus.CONFIRMED)
                        .room(Room.builder().hotel(h1).build())
                        .customer(cust1)
                        .build();
        Booking b2 =
                Booking.builder()
                        .bookingId(UUID.randomUUID())
                        .checkInDate(LocalDate.now().plusDays(1))
                        .bookingStatus(BookingStatus.PENDING)
                        .room(Room.builder().hotel(h1).build())
                        .customer(cust2)
                        .build();
        Booking b3 =
                Booking.builder()
                        .bookingId(UUID.randomUUID())
                        .checkInDate(LocalDate.now().minusDays(2))
                        .bookingStatus(BookingStatus.CONFIRMED)
                        .room(Room.builder().hotel(h1).build())
                        .customer(cust3)
                        .build(); // past

        when(bookingRepository.findByHotelId(h1.getHotelId())).thenReturn(List.of(b1, b2, b3));

        List<com.apexon.stayease.model.BookingResponse> res =
                bookingService.getManagerBookings(managerId);
        assertThat(res).hasSize(2);
        // should be sorted by checkInDate ascending: b2 then b1
        assertThat(res.get(0).getBookingId()).isEqualTo(b2.getBookingId());
        assertThat(res.get(1).getBookingId()).isEqualTo(b1.getBookingId());
    }
}
