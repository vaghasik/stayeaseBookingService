package com.apexon.stayease.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.apexon.stayease.exception.ErrorResponse;
import com.apexon.stayease.model.AuthResponse;
import com.apexon.stayease.model.LoginRequest;
import com.apexon.stayease.model.RegisterRequest;
import com.apexon.stayease.service.AuthService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Authentication Controller
 * REST endpoints for user authentication and authorization.
 *
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Validated
@Slf4j
@Tag(name = "Authentication", description = "APIs for user registration, login, and logout")
public class AuthController {

    private final AuthService authService;

    @Operation(
            summary = "Register a new user",
            description = "Creates a new user account with GUEST role and returns a JWT token")
    @ApiResponses({
        @ApiResponse(
                responseCode = "201",
                description = "User registered successfully",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = AuthResponse.class))),
        @ApiResponse(
                responseCode = "400",
                description = "Invalid request body",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = ErrorResponse.class),
                                examples =
                                        @ExampleObject(
                                                value =
                                                        "{\"timestamp\":\"2026-08-23T10:30:00\",\"path\":\"/api/auth/register\",\"error\":\"Bad Request\",\"message\":\"Validation failed\",\"details\":{\"email\":\"Email must be valid\",\"password\":\"Password must be between 8 and 100 characters\"}}"))),
        @ApiResponse(
                responseCode = "409",
                description = "Email already registered",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = ErrorResponse.class),
                                examples =
                                        @ExampleObject(
                                                value =
                                                        "{\"timestamp\":\"2026-08-23T10:30:00\",\"path\":\"/api/auth/register\",\"error\":\"Conflict\",\"message\":\"Email already registered\"}")))
    })
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        log.info("POST /api/auth/register - Registering new user: {}", request.getEmail());
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
            summary = "Login user",
            description = "Authenticates user with email and password, returns a JWT token")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "Login successful",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = AuthResponse.class))),
        @ApiResponse(
                responseCode = "401",
                description = "Invalid email or password",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = ErrorResponse.class),
                                examples =
                                        @ExampleObject(
                                                value =
                                                        "{\"timestamp\":\"2026-08-23T10:30:00\",\"path\":\"/api/auth/login\",\"error\":\"Unauthorized\",\"message\":\"Invalid email or password\"}")))
    })
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("POST /api/auth/login - User login attempt: {}", request.getEmail());
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Logout user",
            description = "Clears the security context. Client should discard the token")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Logout successful", content = @Content)
    })
    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        log.info("POST /api/auth/logout - User logout");
        authService.logout();
        return ResponseEntity.noContent().build();
    }
}
