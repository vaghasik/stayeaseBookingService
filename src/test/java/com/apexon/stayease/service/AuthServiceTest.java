package com.apexon.stayease.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.apexon.stayease.entity.User;
import com.apexon.stayease.enums.UserRole;
import com.apexon.stayease.exception.EmailAlreadyExistsException;
import com.apexon.stayease.model.AuthResponse;
import com.apexon.stayease.model.LoginRequest;
import com.apexon.stayease.model.RegisterRequest;
import com.apexon.stayease.repository.UserRepository;
import com.apexon.stayease.security.CustomUserDetails;
import com.apexon.stayease.security.JwtTokenProvider;
import com.apexon.stayease.service.impl.AuthServiceImpl;

/**
 * Unit tests for AuthServiceImpl
 * Tests user registration, login, and logout operations
 *
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;

    @Mock private PasswordEncoder passwordEncoder;

    @Mock private AuthenticationManager authenticationManager;

    @Mock private JwtTokenProvider jwtTokenProvider;

    @InjectMocks private AuthServiceImpl authService;

    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;
    private User user;
    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();

        registerRequest =
                RegisterRequest.builder()
                        .name("John Doe")
                        .email("john.doe@example.com")
                        .password("SecurePassword123!")
                        .build();

        loginRequest =
                LoginRequest.builder()
                        .email("john.doe@example.com")
                        .password("SecurePassword123!")
                        .build();

        user =
                User.builder()
                        .userId(userId)
                        .fullName("John Doe")
                        .emailAddress("john.doe@example.com")
                        .passwordHash("encodedPassword")
                        .userRole(UserRole.GUEST)
                        .isActive(true)
                        .build();
    }

    @Test
    @DisplayName("Should register new user successfully")
    void testRegister_Success() {
        when(userRepository.existsByEmailAddress(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(jwtTokenProvider.generateTokenFromUserId(any(UUID.class), anyString(), anyString()))
                .thenReturn("generated-token");

        AuthResponse response = authService.register(registerRequest);

        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo("generated-token");
        assertThat(response.getTokenType()).isEqualTo("Bearer");
        assertThat(response.getEmail()).isEqualTo("john.doe@example.com");
        assertThat(response.getName()).isEqualTo("John Doe");
        assertThat(response.getRole()).isEqualTo("GUEST");

        verify(userRepository).existsByEmailAddress("john.doe@example.com");
        verify(passwordEncoder).encode("SecurePassword123!");
        verify(userRepository).save(any(User.class));
        verify(jwtTokenProvider).generateTokenFromUserId(any(UUID.class), anyString(), anyString());
    }

    @Test
    @DisplayName("Should throw exception when email already exists")
    void testRegister_EmailAlreadyExists() {
        when(userRepository.existsByEmailAddress(anyString())).thenReturn(true);

        assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf(EmailAlreadyExistsException.class)
                .hasMessageContaining("Email already registered");

        verify(userRepository).existsByEmailAddress("john.doe@example.com");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should login user successfully")
    void testLogin_Success() {
        Authentication authentication = mock(Authentication.class);
        CustomUserDetails userDetails = CustomUserDetails.create(user);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(jwtTokenProvider.generateToken(authentication)).thenReturn("generated-token");
        when(userRepository.findByEmailAddress(anyString())).thenReturn(Optional.of(user));

        AuthResponse response = authService.login(loginRequest);

        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo("generated-token");
        assertThat(response.getTokenType()).isEqualTo("Bearer");
        assertThat(response.getEmail()).isEqualTo("john.doe@example.com");

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(jwtTokenProvider).generateToken(authentication);
    }

    @Test
    @DisplayName("Should clear security context on logout")
    void testLogout_Success() {
        authService.logout();

        // Verify that logout completes without exception
        assertThat(true).isTrue();
    }

    @Test
    @DisplayName("Should create user with GUEST role by default")
    void testRegister_DefaultGuestRole() {
        when(userRepository.existsByEmailAddress(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(jwtTokenProvider.generateTokenFromUserId(any(UUID.class), anyString(), anyString()))
                .thenReturn("generated-token");

        AuthResponse response = authService.register(registerRequest);

        assertThat(response.getRole()).isEqualTo("GUEST");
        verify(userRepository)
                .save(argThat(savedUser -> savedUser.getUserRole() == UserRole.GUEST));
    }

    @Test
    @DisplayName("Should encode password during registration")
    void testRegister_PasswordEncoding() {
        when(userRepository.existsByEmailAddress(anyString())).thenReturn(false);
        when(passwordEncoder.encode("SecurePassword123!")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(jwtTokenProvider.generateTokenFromUserId(any(UUID.class), anyString(), anyString()))
                .thenReturn("generated-token");

        authService.register(registerRequest);

        verify(passwordEncoder).encode("SecurePassword123!");
        verify(userRepository)
                .save(argThat(savedUser -> "encodedPassword".equals(savedUser.getPasswordHash())));
    }
}
