package com.apexon.stayease.util;

import static com.apexon.stayease.constant.AppConstants.*;

import java.time.LocalDateTime;
import java.util.Random;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Booking Reference Generator Utility
 * Generates unique booking references using Factory pattern.
 *
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class BookingReferenceGenerator {

    private static final Random RANDOM = new Random();

    /**
     * Generates a unique booking reference
     * Format: BK-YYYYMMDD-XXXXXXXX
     * Example: BK-20260825-A3F5K7M2
     *
     * @return Generated booking reference
     */
    public static String generate() {
        String datePart = LocalDateTime.now().format(BOOKING_DATE_FORMATTER);
        String randomPart = generateRandomString(BOOKING_REF_RANDOM_LENGTH);
        return String.format("%s-%s-%s", BOOKING_REF_PREFIX, datePart, randomPart);
    }

    /**
     * Generates a random alphanumeric string
     *
     * @param length Length of the random string
     * @return Random alphanumeric string
     */
    private static String generateRandomString(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int index = RANDOM.nextInt(ALPHANUMERIC_CHARACTERS.length());
            sb.append(ALPHANUMERIC_CHARACTERS.charAt(index));
        }
        return sb.toString();
    }

    /**
     * Validates if a booking reference follows the expected format
     *
     * @param reference Booking reference to validate
     * @return true if valid format, false otherwise
     */
    public static boolean isValidFormat(String reference) {
        if (reference == null || reference.isEmpty()) {
            return false;
        }
        return reference.matches(BOOKING_REF_PATTERN);
    }
}
