package com.apexon.stayease.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class BookingReferenceGeneratorTest {

    @Test
    @DisplayName("Should generate reference with correct format BK-YYYYMMDD-XXXXXXXX")
    void testGenerate_Format() {
        String ref = BookingReferenceGenerator.generate();

        assertThat(ref).matches("^BK-\\d{8}-[A-Z0-9]{8}$");
    }

    @Test
    @DisplayName("Should generate reference starting with BK prefix")
    void testGenerate_Prefix() {
        String ref = BookingReferenceGenerator.generate();

        assertThat(ref).startsWith("BK-");
    }

    @Test
    @DisplayName("Should generate reference with exactly 20 characters")
    void testGenerate_Length() {
        String ref = BookingReferenceGenerator.generate();

        // BK-YYYYMMDD-XXXXXXXX = 2 + 1 + 8 + 1 + 8 = 20
        assertThat(ref).hasSize(20);
    }

    @Test
    @DisplayName("Should generate unique references")
    void testGenerate_Uniqueness() {
        Set<String> refs = new HashSet<>();
        for (int i = 0; i < 100; i++) {
            refs.add(BookingReferenceGenerator.generate());
        }
        // With 36^8 possible combos, 100 should all be unique
        assertThat(refs).hasSize(100);
    }

    @Test
    @DisplayName("Should validate correct format")
    void testIsValidFormat_Valid() {
        assertThat(BookingReferenceGenerator.isValidFormat("BK-20260825-ABCD1234")).isTrue();
        assertThat(BookingReferenceGenerator.isValidFormat("BK-20261231-Z9Y8X7W6")).isTrue();
    }

    @Test
    @DisplayName("Should reject invalid formats")
    void testIsValidFormat_Invalid() {
        assertThat(BookingReferenceGenerator.isValidFormat(null)).isFalse();
        assertThat(BookingReferenceGenerator.isValidFormat("")).isFalse();
        assertThat(BookingReferenceGenerator.isValidFormat("INVALID")).isFalse();
        assertThat(BookingReferenceGenerator.isValidFormat("BK-2026082-ABCD1234"))
                .isFalse(); // 7 digit date
        assertThat(BookingReferenceGenerator.isValidFormat("BK-20260825-abcd1234"))
                .isFalse(); // lowercase
        assertThat(BookingReferenceGenerator.isValidFormat("XX-20260825-ABCD1234"))
                .isFalse(); // wrong prefix
    }

    @Test
    @DisplayName("Generated references should pass validation")
    void testGenerate_PassesValidation() {
        for (int i = 0; i < 10; i++) {
            String ref = BookingReferenceGenerator.generate();
            assertThat(BookingReferenceGenerator.isValidFormat(ref))
                    .as("Generated ref '%s' should be valid", ref)
                    .isTrue();
        }
    }
}
