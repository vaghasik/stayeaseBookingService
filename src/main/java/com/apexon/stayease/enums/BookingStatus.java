package com.apexon.stayease.enums;

/**
 * Booking Status Enum
 * Defines the different states of a booking in its lifecycle.
 *
 */
public enum BookingStatus {
    PENDING, // Booking created but not yet confirmed
    CONFIRMED, // Booking confirmed and payment received
    CANCELLED, // Booking cancelled by user or system
    COMPLETED // Booking completed (after check-out)
}
