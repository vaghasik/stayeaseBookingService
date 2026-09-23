package com.apexon.stayease.entity;

import java.io.Serializable;
import java.util.UUID;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * HotelAmenity Entity
 * Represents amenities offered by a hotel (e.g., WiFi, Pool, Gym, etc.)
 * Uses composite primary key (hotel_id, amenity_name).
 *
 */
@Entity
@Table(
        name = "hotel_amenities",
        indexes = {@Index(name = "idx_hotel_amenities_hotel_id", columnList = "hotel_id")})
@IdClass(HotelAmenity.HotelAmenityId.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "hotel")
@EqualsAndHashCode(of = {"hotelId", "amenityName"})
public class HotelAmenity {

    @Id
    @Column(name = "hotel_id", nullable = false)
    private UUID hotelId;

    @Id
    @NotBlank(message = "Amenity name is required")
    @Size(max = 50, message = "Amenity name must not exceed 50 characters")
    @Column(name = "amenity_name", nullable = false, length = 50)
    private String amenityName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "hotel_id",
            insertable = false,
            updatable = false,
            foreignKey = @ForeignKey(name = "hotel_amenities_hotel_fk"))
    private Hotel hotel;

    /**
     * Composite Primary Key Class for HotelAmenity
     *
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HotelAmenityId implements Serializable {
        private UUID hotelId;
        private String amenityName;
    }
}
