package com.apexon.stayease.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

import com.apexon.stayease.model.AuthResponse;
import com.apexon.stayease.model.LoginRequest;
import com.apexon.stayease.model.RegisterRequest;
import com.apexon.stayease.service.AuthService;

/**
 * Unit tests for AuthController
 * Tests REST endpoints for authentication operations using JUnit5 + Mockito
 *
 */
@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock private AuthService authService;

    @InjectMocks private AuthController authController;

    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;
    private AuthResponse authResponse;
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
        authResponse =
                AuthResponse.builder()
                        .token("jwt-token-value")
                        .tokenType("Bearer")
                        .userId(userId)
                        .email("john.doe@example.com")
                        .name("John Doe")
                        .role("GUEST")
                        .build();
    }

    @Test
    @DisplayName("Should register user and return CREATED status")
    void testRegister_Success() {
        when(authService.register(any(RegisterRequest.class))).thenReturn(authResponse);

        ResponseEntity<AuthResponse> response = authController.register(registerRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getToken()).isEqualTo("jwt-token-value");
        assertThat(response.getBody().getTokenType()).isEqualTo("Bearer");
        assertThat(response.getBody().getEmail()).isEqualTo("john.doe@example.com");
        assertThat(response.getBody().getName()).isEqualTo("John Doe");
        assertThat(response.getBody().getRole()).isEqualTo("GUEST");
        assertThat(response.getBody().getUserId()).isEqualTo(userId);
        verify(authService).register(any(RegisterRequest.class));
    }

    @Test
    @DisplayName("Should login user and return OK status")
    void testLogin_Success() {
        when(authService.login(any(LoginRequest.class))).thenReturn(authResponse);

        ResponseEntity<AuthResponse> response = authController.login(loginRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getToken()).isEqualTo("jwt-token-value");
        assertThat(response.getBody().getTokenType()).isEqualTo("Bearer");
        assertThat(response.getBody().getEmail()).isEqualTo("john.doe@example.com");
        verify(authService).login(any(LoginRequest.class));
    }

    @Test
    @DisplayName("Should logout user and return NO_CONTENT status")
    void testLogout_Success() {
        doNothing().when(authService).logout();

        ResponseEntity<Void> response = authController.logout();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(response.getBody()).isNull();
        verify(authService).logout();
    }

    @Test
    @DisplayName("Should pass register request to service")
    void testRegister_DelegateToService() {
        when(authService.register(registerRequest)).thenReturn(authResponse);

        authController.register(registerRequest);

        verify(authService).register(registerRequest);
    }

    @Test
    @DisplayName("Should pass login request to service")
    void testLogin_DelegateToService() {
        when(authService.login(loginRequest)).thenReturn(authResponse);

        authController.login(loginRequest);

        verify(authService).login(loginRequest);
    }
}
