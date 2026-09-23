package com.apexon.stayease.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

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

import com.apexon.stayease.entity.Hotel;
import com.apexon.stayease.entity.User;
import com.apexon.stayease.enums.UserRole;
import com.apexon.stayease.exception.ResourceNotFoundException;
import com.apexon.stayease.model.HotelRequest;
import com.apexon.stayease.model.HotelResponse;
import com.apexon.stayease.repository.HotelRepository;
import com.apexon.stayease.repository.UserRepository;
import com.apexon.stayease.service.impl.HotelServiceImpl;

@ExtendWith(MockitoExtension.class)
class HotelServiceTest {

    @Mock private HotelRepository hotelRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks private HotelServiceImpl hotelService;

    private UUID managerId;
    private UUID hotelId;

    @BeforeEach
    void setUp() {
        managerId = UUID.randomUUID();
        hotelId = UUID.randomUUID();
    }

    @Test
    @DisplayName("getHotelsByCity returns mapped responses")
    void testGetHotelsByCity() {
        Hotel h =
                Hotel.builder()
                        .hotelId(hotelId)
                        .hotelName("H1")
                        .cityName("CityZ")
                        .isActive(true)
                        .manager(User.builder().userId(managerId).fullName("M").build())
                        .build();
        when(hotelRepository.findByCityNameAndIsActive("CityZ", true)).thenReturn(List.of(h));

        List<HotelResponse> res = hotelService.getHotelsByCity("CityZ");
        assertThat(res).hasSize(1);
        assertThat(res.get(0).getHotelName()).isEqualTo("H1");
    }

    @Test
    @DisplayName("getHotelsByManagerId returns mapped responses")
    void testGetHotelsByManagerId() {
        Hotel h =
                Hotel.builder()
                        .hotelId(hotelId)
                        .hotelName("HM")
                        .manager(User.builder().userId(managerId).fullName("Mgr").build())
                        .build();
        when(hotelRepository.findByManagerId(managerId)).thenReturn(List.of(h));

        List<HotelResponse> res = hotelService.getHotelsByManagerId(managerId);
        assertThat(res).hasSize(1);
        assertThat(res.get(0).getManagerId()).isEqualTo(managerId);
    }

    @Test
    @DisplayName("getAllHotels returns mapped list")
    void testGetAllHotels() {
        Hotel h =
                Hotel.builder()
                        .hotelId(hotelId)
                        .hotelName("AllH")
                        .manager(User.builder().userId(managerId).fullName("Mgr").build())
                        .build();
        when(hotelRepository.findAll()).thenReturn(List.of(h));

        List<HotelResponse> res = hotelService.getAllHotels();
        assertThat(res).hasSize(1);
        assertThat(res.get(0).getHotelName()).isEqualTo("AllH");
    }

    @Test
    @DisplayName("getHotelById - success")
    void testGetHotelById_Success() {
        Hotel h =
                Hotel.builder()
                        .hotelId(hotelId)
                        .hotelName("ById")
                        .manager(User.builder().userId(managerId).fullName("Mgr").build())
                        .build();
        when(hotelRepository.findById(hotelId)).thenReturn(Optional.of(h));

        HotelResponse resp = hotelService.getHotelById(hotelId);
        assertThat(resp.getHotelId()).isEqualTo(hotelId);
        assertThat(resp.getHotelName()).isEqualTo("ById");
    }

    @Test
    @DisplayName("getHotelById - not found")
    void testGetHotelById_NotFound() {
        when(hotelRepository.findById(hotelId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> hotelService.getHotelById(hotelId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("createHotel - success when manager is MANAGER")
    void testCreateHotel_Success_ManagerRole() {
        HotelRequest req =
                HotelRequest.builder()
                        .hotelName("NewH")
                        .cityName("C")
                        .countryName("Country")
                        .managerId(managerId)
                        .build();

        User mgr =
                User.builder().userId(managerId).fullName("Mgr").userRole(UserRole.MANAGER).build();
        when(userRepository.findById(managerId)).thenReturn(Optional.of(mgr));

        Hotel saved = Hotel.builder().hotelId(hotelId).hotelName("NewH").manager(mgr).build();
        when(hotelRepository.save(any(Hotel.class))).thenReturn(saved);

        HotelResponse resp = hotelService.createHotel(req);
        assertThat(resp.getHotelId()).isEqualTo(hotelId);
        assertThat(resp.getManagerId()).isEqualTo(managerId);
    }

    @Test
    @DisplayName("createHotel - manager not found")
    void testCreateHotel_ManagerNotFound() {
        HotelRequest req =
                HotelRequest.builder()
                        .managerId(managerId)
                        .hotelName("NH")
                        .cityName("C")
                        .countryName("Co")
                        .build();
        when(userRepository.findById(managerId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> hotelService.createHotel(req))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("createHotel - manager role invalid")
    void testCreateHotel_ManagerRoleInvalid() {
        HotelRequest req =
                HotelRequest.builder()
                        .managerId(managerId)
                        .hotelName("NH")
                        .cityName("C")
                        .countryName("Co")
                        .build();
        User usr = User.builder().userId(managerId).fullName("U").userRole(UserRole.GUEST).build();
        when(userRepository.findById(managerId)).thenReturn(Optional.of(usr));
        assertThatThrownBy(() -> hotelService.createHotel(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("MANAGER or ADMIN");
    }

    @Test
    @DisplayName("updateHotel - change fields and keep manager")
    void testUpdateHotel_UpdateFields() {
        Hotel existing =
                Hotel.builder()
                        .hotelId(hotelId)
                        .hotelName("Old")
                        .manager(User.builder().userId(managerId).fullName("Mgr").build())
                        .isActive(true)
                        .build();
        when(hotelRepository.findById(hotelId)).thenReturn(Optional.of(existing));

        HotelRequest req =
                HotelRequest.builder()
                        .hotelName("Updated")
                        .cityName("CityU")
                        .countryName("Co")
                        .build();
        when(hotelRepository.save(existing)).thenReturn(existing);

        HotelResponse resp = hotelService.updateHotel(hotelId, req);
        assertThat(resp.getHotelName()).isEqualTo("Updated");
    }

    @Test
    @DisplayName("updateHotel - change manager to new manager")
    void testUpdateHotel_ChangeManager_Success() {
        UUID newMgrId = UUID.randomUUID();
        User oldMgr =
                User.builder()
                        .userId(managerId)
                        .fullName("OldMgr")
                        .userRole(UserRole.MANAGER)
                        .build();
        Hotel existing = Hotel.builder().hotelId(hotelId).hotelName("H").manager(oldMgr).build();
        when(hotelRepository.findById(hotelId)).thenReturn(Optional.of(existing));

        User newMgr =
                User.builder().userId(newMgrId).fullName("NewMgr").userRole(UserRole.ADMIN).build();
        when(userRepository.findById(newMgrId)).thenReturn(Optional.of(newMgr));

        HotelRequest req =
                HotelRequest.builder()
                        .managerId(newMgrId)
                        .hotelName("Hnew")
                        .cityName("C")
                        .countryName("Co")
                        .build();
        when(hotelRepository.save(existing)).thenReturn(existing);

        HotelResponse resp = hotelService.updateHotel(hotelId, req);
        assertThat(resp.getManagerId()).isEqualTo(newMgrId);
    }

    @Test
    @DisplayName("updateHotel - change manager not found")
    void testUpdateHotel_ChangeManager_NotFound() {
        UUID newMgrId = UUID.randomUUID();
        Hotel existing =
                Hotel.builder()
                        .hotelId(hotelId)
                        .hotelName("H")
                        .manager(User.builder().userId(managerId).fullName("Mgr").build())
                        .build();
        when(hotelRepository.findById(hotelId)).thenReturn(Optional.of(existing));

        when(userRepository.findById(newMgrId)).thenReturn(Optional.empty());
        HotelRequest req =
                HotelRequest.builder()
                        .managerId(newMgrId)
                        .hotelName("HN")
                        .cityName("C")
                        .countryName("Co")
                        .build();

        assertThatThrownBy(() -> hotelService.updateHotel(hotelId, req))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("updateHotel - change manager invalid role")
    void testUpdateHotel_ChangeManager_InvalidRole() {
        UUID newMgrId = UUID.randomUUID();
        User oldMgr =
                User.builder()
                        .userId(managerId)
                        .fullName("OldMgr")
                        .userRole(UserRole.MANAGER)
                        .build();
        Hotel existing = Hotel.builder().hotelId(hotelId).hotelName("H").manager(oldMgr).build();
        when(hotelRepository.findById(hotelId)).thenReturn(Optional.of(existing));

        User newUser =
                User.builder().userId(newMgrId).fullName("NotMgr").userRole(UserRole.GUEST).build();
        when(userRepository.findById(newMgrId)).thenReturn(Optional.of(newUser));

        HotelRequest req =
                HotelRequest.builder()
                        .managerId(newMgrId)
                        .hotelName("HN")
                        .cityName("C")
                        .countryName("Co")
                        .build();
        assertThatThrownBy(() -> hotelService.updateHotel(hotelId, req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("MANAGER or ADMIN");
    }

    @Test
    @DisplayName("deleteHotel - success")
    void testDeleteHotel_Success() {
        Hotel existing = Hotel.builder().hotelId(hotelId).hotelName("Del").build();
        when(hotelRepository.findById(hotelId)).thenReturn(Optional.of(existing));

        hotelService.deleteHotel(hotelId);
        verify(hotelRepository).delete(existing);
    }

    @Test
    @DisplayName("deleteHotel - not found")
    void testDeleteHotel_NotFound() {
        when(hotelRepository.findById(hotelId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> hotelService.deleteHotel(hotelId))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
