package com.apexon.stayease.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import com.apexon.stayease.entity.User;
import com.apexon.stayease.enums.UserRole;

import jakarta.servlet.ServletException;

/**
 * Unit tests for JwtAuthenticationFilter
 * Tests JWT token validation and authentication
 *
 */
@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock private JwtTokenProvider tokenProvider;

    @Mock private CustomUserDetailsService customUserDetailsService;

    @InjectMocks private JwtAuthenticationFilter jwtAuthenticationFilter;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private MockFilterChain filterChain;
    private String validToken;
    private UUID userId;
    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        filterChain = new MockFilterChain();
        validToken = "valid.jwt.token";
        userId = UUID.randomUUID();

        User user =
                User.builder()
                        .userId(userId)
                        .emailAddress("test@example.com")
                        .fullName("Test User")
                        .passwordHash("encodedPassword")
                        .userRole(UserRole.GUEST)
                        .isActive(true)
                        .build();

        userDetails = CustomUserDetails.create(user);
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Should authenticate user with valid JWT token")
    void testDoFilterInternal_ValidToken() throws ServletException, IOException {
        request.addHeader("Authorization", "Bearer " + validToken);

        when(tokenProvider.validateToken(validToken)).thenReturn(true);
        when(tokenProvider.getUserIdFromToken(validToken)).thenReturn(userId);
        when(customUserDetailsService.loadUserById(userId)).thenReturn(userDetails);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal())
                .isEqualTo(userDetails);

        verify(tokenProvider).validateToken(validToken);
        verify(tokenProvider).getUserIdFromToken(validToken);
        verify(customUserDetailsService).loadUserById(userId);
    }

    @Test
    @DisplayName("Should not authenticate with invalid token")
    void testDoFilterInternal_InvalidToken() throws ServletException, IOException {
        request.addHeader("Authorization", "Bearer invalid.token");

        when(tokenProvider.validateToken(anyString())).thenReturn(false);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();

        verify(tokenProvider).validateToken(anyString());
        verify(customUserDetailsService, never()).loadUserById(any());
    }

    @Test
    @DisplayName("Should not authenticate without Authorization header")
    void testDoFilterInternal_NoAuthHeader() throws ServletException, IOException {
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();

        verify(tokenProvider, never()).validateToken(anyString());
        verify(customUserDetailsService, never()).loadUserById(any());
    }

    @Test
    @DisplayName("Should not authenticate with malformed Authorization header")
    void testDoFilterInternal_MalformedAuthHeader() throws ServletException, IOException {
        request.addHeader("Authorization", "InvalidFormat token");

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();

        verify(tokenProvider, never()).validateToken(anyString());
    }

    @Test
    @DisplayName("Should handle exception during authentication gracefully")
    void testDoFilterInternal_ExceptionHandling() throws ServletException, IOException {
        request.addHeader("Authorization", "Bearer " + validToken);

        when(tokenProvider.validateToken(validToken)).thenReturn(true);
        when(tokenProvider.getUserIdFromToken(validToken))
                .thenThrow(new RuntimeException("Token error"));

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();

        verify(tokenProvider).validateToken(validToken);
    }

    @Test
    @DisplayName("Should extract token from Bearer header correctly")
    void testDoFilterInternal_ExtractToken() throws ServletException, IOException {
        String token = "extracted.token.value";
        request.addHeader("Authorization", "Bearer " + token);

        when(tokenProvider.validateToken(token)).thenReturn(true);
        when(tokenProvider.getUserIdFromToken(token)).thenReturn(userId);
        when(customUserDetailsService.loadUserById(userId)).thenReturn(userDetails);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(tokenProvider).validateToken(token);
    }

    @Test
    @DisplayName("Should not process empty Bearer token")
    void testDoFilterInternal_EmptyBearerToken() throws ServletException, IOException {
        request.addHeader("Authorization", "Bearer ");

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(tokenProvider, never()).validateToken(anyString());
    }

    @Test
    @DisplayName("Should set authentication details in security context")
    void testDoFilterInternal_SetAuthenticationDetails() throws ServletException, IOException {
        request.addHeader("Authorization", "Bearer " + validToken);
        request.setRemoteAddr("127.0.0.1");

        when(tokenProvider.validateToken(validToken)).thenReturn(true);
        when(tokenProvider.getUserIdFromToken(validToken)).thenReturn(userId);
        when(customUserDetailsService.loadUserById(userId)).thenReturn(userDetails);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getDetails()).isNotNull();
    }
}
