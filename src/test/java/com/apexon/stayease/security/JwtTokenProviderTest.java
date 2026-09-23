package com.apexon.stayease.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.TestPropertySource;

import com.apexon.stayease.entity.User;
import com.apexon.stayease.enums.UserRole;

/**
 * Unit tests for JwtTokenProvider
 * Tests JWT token generation, validation, and parsing
 *
 */
@SpringBootTest(classes = JwtTokenProvider.class)
@TestPropertySource(
        properties = {
            "app.jwt.secret=dGVzdC1zZWNyZXQta2V5LWZvci1qd3QtdG9rZW4tdGVzdGluZy1wdXJwb3Nlcy1vbmx5LW11c3QtYmUtbG9uZy1lbm91Z2gtdG8tbWVldC1yZXF1aXJlbWVudHM=",
            "app.jwt.expiration-ms=3600000"
        })
class JwtTokenProviderTest {

    @Autowired private JwtTokenProvider jwtTokenProvider;

    private UUID userId;
    private String email;
    private String role;
    private CustomUserDetails userDetails;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        email = "test@example.com";
        role = "GUEST";

        User user =
                User.builder()
                        .userId(userId)
                        .emailAddress(email)
                        .fullName("Test User")
                        .passwordHash("encodedPassword")
                        .userRole(UserRole.GUEST)
                        .isActive(true)
                        .build();

        userDetails = CustomUserDetails.create(user);
    }

    @Test
    @DisplayName("Should generate token from authentication")
    void testGenerateToken_FromAuthentication() {
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(userDetails);

        String token = jwtTokenProvider.generateToken(authentication);

        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();
        assertThat(token.split("\\.")).hasSize(3); // JWT has 3 parts
    }

    @Test
    @DisplayName("Should generate token from user ID")
    void testGenerateToken_FromUserId() {
        String token = jwtTokenProvider.generateTokenFromUserId(userId, email, role);

        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();
        assertThat(token.split("\\.")).hasSize(3);
    }

    @Test
    @DisplayName("Should validate valid token")
    void testValidateToken_ValidToken() {
        String token = jwtTokenProvider.generateTokenFromUserId(userId, email, role);

        boolean isValid = jwtTokenProvider.validateToken(token);

        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("Should reject invalid token")
    void testValidateToken_InvalidToken() {
        String invalidToken = "invalid.token.here";

        boolean isValid = jwtTokenProvider.validateToken(invalidToken);

        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Should reject malformed token")
    void testValidateToken_MalformedToken() {
        String malformedToken = "malformed-token";

        boolean isValid = jwtTokenProvider.validateToken(malformedToken);

        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Should extract user ID from token")
    void testGetUserIdFromToken() {
        String token = jwtTokenProvider.generateTokenFromUserId(userId, email, role);

        UUID extractedUserId = jwtTokenProvider.getUserIdFromToken(token);

        assertThat(extractedUserId).isEqualTo(userId);
    }

    @Test
    @DisplayName("Should generate different tokens for different users")
    void testGenerateToken_DifferentUsers() {
        UUID userId1 = UUID.randomUUID();
        UUID userId2 = UUID.randomUUID();

        String token1 =
                jwtTokenProvider.generateTokenFromUserId(userId1, "user1@example.com", "GUEST");
        String token2 =
                jwtTokenProvider.generateTokenFromUserId(userId2, "user2@example.com", "ADMIN");

        assertThat(token1).isNotEqualTo(token2);
    }

    @Test
    @DisplayName("Should generate token with correct structure")
    void testGenerateToken_Structure() {
        String token = jwtTokenProvider.generateTokenFromUserId(userId, email, role);

        String[] parts = token.split("\\.");
        assertThat(parts).hasSize(3); // Header, Payload, Signature
        assertThat(parts[0]).isNotEmpty(); // Header
        assertThat(parts[1]).isNotEmpty(); // Payload
        assertThat(parts[2]).isNotEmpty(); // Signature
    }

    @Test
    @DisplayName("Should handle null token validation gracefully")
    void testValidateToken_NullToken() {
        boolean isValid = jwtTokenProvider.validateToken(null);

        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Should handle empty token validation gracefully")
    void testValidateToken_EmptyToken() {
        boolean isValid = jwtTokenProvider.validateToken("");

        assertThat(isValid).isFalse();
    }
}
