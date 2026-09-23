package com.apexon.stayease.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import com.apexon.stayease.entity.User;
import com.apexon.stayease.enums.UserRole;
import com.apexon.stayease.repository.UserRepository;

/**
 * Unit tests for CustomUserDetailsService
 * Tests user loading by username and ID
 *
 */
@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock private UserRepository userRepository;

    @InjectMocks private CustomUserDetailsService customUserDetailsService;

    private User user;
    private UUID userId;
    private String email;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        email = "test@example.com";

        user =
                User.builder()
                        .userId(userId)
                        .emailAddress(email)
                        .fullName("Test User")
                        .passwordHash("encodedPassword")
                        .userRole(UserRole.GUEST)
                        .isActive(true)
                        .build();
    }

    @Test
    @DisplayName("Should load user by username successfully")
    void testLoadUserByUsername_Success() {
        when(userRepository.findByEmailAddress(email)).thenReturn(Optional.of(user));

        UserDetails userDetails = customUserDetailsService.loadUserByUsername(email);

        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getUsername()).isEqualTo(email);
        assertThat(userDetails).isInstanceOf(CustomUserDetails.class);

        CustomUserDetails customUserDetails = (CustomUserDetails) userDetails;
        assertThat(customUserDetails.getUserId()).isEqualTo(userId);
        assertThat(customUserDetails.getEmail()).isEqualTo(email);

        verify(userRepository).findByEmailAddress(email);
    }

    @Test
    @DisplayName("Should throw exception when user not found by username")
    void testLoadUserByUsername_UserNotFound() {
        when(userRepository.findByEmailAddress(email)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customUserDetailsService.loadUserByUsername(email))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("User not found with email: " + email);

        verify(userRepository).findByEmailAddress(email);
    }

    @Test
    @DisplayName("Should load user by ID successfully")
    void testLoadUserById_Success() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        UserDetails userDetails = customUserDetailsService.loadUserById(userId);

        assertThat(userDetails).isNotNull();
        assertThat(userDetails).isInstanceOf(CustomUserDetails.class);

        CustomUserDetails customUserDetails = (CustomUserDetails) userDetails;
        assertThat(customUserDetails.getUserId()).isEqualTo(userId);
        assertThat(customUserDetails.getEmail()).isEqualTo(email);

        verify(userRepository).findById(userId);
    }

    @Test
    @DisplayName("Should throw exception when user not found by ID")
    void testLoadUserById_UserNotFound() {
        when(userRepository.findById(any(UUID.class))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customUserDetailsService.loadUserById(userId))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("User not found with id: " + userId);

        verify(userRepository).findById(userId);
    }

    @Test
    @DisplayName("Should return user with correct authorities")
    void testLoadUserByUsername_WithAuthorities() {
        when(userRepository.findByEmailAddress(email)).thenReturn(Optional.of(user));

        UserDetails userDetails = customUserDetailsService.loadUserByUsername(email);

        assertThat(userDetails.getAuthorities()).isNotEmpty();
        assertThat(userDetails.getAuthorities()).hasSize(1);
        assertThat(userDetails.getAuthorities().iterator().next().getAuthority())
                .isEqualTo("GUEST");
    }

    @Test
    @DisplayName("Should return enabled user account")
    void testLoadUserByUsername_EnabledAccount() {
        when(userRepository.findByEmailAddress(email)).thenReturn(Optional.of(user));

        UserDetails userDetails = customUserDetailsService.loadUserByUsername(email);

        assertThat(userDetails.isEnabled()).isTrue();
        assertThat(userDetails.isAccountNonExpired()).isTrue();
        assertThat(userDetails.isAccountNonLocked()).isTrue();
        assertThat(userDetails.isCredentialsNonExpired()).isTrue();
    }

    @Test
    @DisplayName("Should handle disabled user account")
    void testLoadUserByUsername_DisabledAccount() {
        user.setIsActive(false);
        when(userRepository.findByEmailAddress(email)).thenReturn(Optional.of(user));

        UserDetails userDetails = customUserDetailsService.loadUserByUsername(email);

        assertThat(userDetails.isEnabled()).isFalse();
    }
}
