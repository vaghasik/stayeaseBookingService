package com.apexon.stayease.service;

import com.apexon.stayease.model.AuthResponse;
import com.apexon.stayease.model.LoginRequest;
import com.apexon.stayease.model.RegisterRequest;

/**
 * Authentication Service Interface
 * Defines contract for user registration, login, and logout operations.
 *
 */
public interface AuthService {
    /**
     * Register a new user
     */
    AuthResponse register(RegisterRequest request);

    /**
     * Authenticate user and generate JWT token
     */
    AuthResponse login(LoginRequest request);

    /**
     * Logout user (client-side token removal)
     */
    void logout();
}
