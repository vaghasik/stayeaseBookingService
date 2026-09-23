package com.apexon.stayease.service.impl;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.apexon.stayease.entity.User;
import com.apexon.stayease.enums.UserRole;
import com.apexon.stayease.exception.EmailAlreadyExistsException;
import com.apexon.stayease.model.AuthResponse;
import com.apexon.stayease.model.LoginRequest;
import com.apexon.stayease.model.RegisterRequest;
import com.apexon.stayease.repository.UserRepository;
import com.apexon.stayease.security.CustomUserDetails;
import com.apexon.stayease.security.JwtTokenProvider;
import com.apexon.stayease.service.AuthService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Authentication Service Implementation
 * Handles user registration, login, and logout operations.
 *
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        log.info("Attempting to register user with email: {}", request.getEmail());
        // Check if email already exists
        if (userRepository.existsByEmailAddress(request.getEmail())) {
            throw new EmailAlreadyExistsException(
                    "Email already registered: " + request.getEmail());
        }
        // Create new user with GUEST role by default
        User user =
                User.builder()
                        .fullName(request.getName())
                        .emailAddress(request.getEmail())
                        .passwordHash(passwordEncoder.encode(request.getPassword()))
                        .userRole(UserRole.GUEST)
                        .isActive(true)
                        .build();
        User savedUser = userRepository.save(user);
        log.info("User registered successfully with ID: {}", savedUser.getUserId());
        // Generate JWT token
        String token =
                jwtTokenProvider.generateTokenFromUserId(
                        savedUser.getUserId(),
                        savedUser.getEmailAddress(),
                        savedUser.getUserRole().name());
        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .userId(savedUser.getUserId())
                .email(savedUser.getEmailAddress())
                .name(savedUser.getFullName())
                .role(savedUser.getUserRole().name())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        log.info("Attempting to authenticate user: {}", request.getEmail());
        // Authenticate user
        Authentication authentication =
                authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(
                                request.getEmail(), request.getPassword()));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        // Generate JWT token
        String token = jwtTokenProvider.generateToken(authentication);
        // Get user details
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        log.info("User authenticated successfully: {}", request.getEmail());
        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .userId(userDetails.getUserId())
                .email(userDetails.getEmail())
                .name(
                        userRepository
                                .findByEmailAddress(userDetails.getEmail())
                                .map(User::getFullName)
                                .orElse(""))
                .role(userDetails.getRole())
                .build();
    }

    @Override
    public void logout() {
        SecurityContextHolder.clearContext();
        log.info("User logged out successfully");
    }
}
