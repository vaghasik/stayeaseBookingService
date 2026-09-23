package com.apexon.stayease.constant;

import java.time.format.DateTimeFormatter;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Application-wide constants for StayEase Booking Service.
 *
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class AppConstants {

    // ── Booking Reference ─────────────────────────────────────
    public static final String BOOKING_REF_PREFIX = "BK";
    public static final DateTimeFormatter BOOKING_DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMdd");
    public static final String ALPHANUMERIC_CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    public static final int BOOKING_REF_RANDOM_LENGTH = 8;
    public static final String BOOKING_REF_PATTERN = "^BK-\\d{8}-[A-Z0-9]{8}$";

    // ── Booking Rules ─────────────────────────────────────────
    public static final int MAX_BOOKING_NIGHTS = 30;

    // ── JWT ───────────────────────────────────────────────────
    public static final String AUTH_HEADER = "Authorization";
    public static final String BEARER_PREFIX = "Bearer ";

    // ── Roles ─────────────────────────────────────────────────
    public static final String ROLE_GUEST = "GUEST";
    public static final String ROLE_MANAGER = "MANAGER";
    public static final String ROLE_ADMIN = "ADMIN";
}
