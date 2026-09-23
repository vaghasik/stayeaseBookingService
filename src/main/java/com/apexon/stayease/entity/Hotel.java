package com.apexon.stayease.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

/**
 * Hotel Entity
 * Represents a hotel property in the StayEase system.
 *
 */
@Entity
@Table(
        name = "hotels",
        indexes = {
            @Index(name = "idx_hotels_city", columnList = "city_name"),
            @Index(name = "idx_hotels_manager", columnList = "manager_id")
        })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"manager", "rooms", "amenities"})
@EqualsAndHashCode(exclude = {"manager", "rooms", "amenities"})
public class Hotel {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "hotel_id", updatable = false, nullable = false)
    private UUID hotelId;

    @NotBlank(message = "Hotel name is required")
    @Size(max = 150, message = "Hotel name must not exceed 150 characters")
    @Column(name = "hotel_name", nullable = false, length = 150)
    private String hotelName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Size(max = 255, message = "Street address must not exceed 255 characters")
    @Column(name = "street_address", length = 255)
    private String streetAddress;

    @NotBlank(message = "City name is required")
    @Size(max = 100, message = "City name must not exceed 100 characters")
    @Column(name = "city_name", nullable = false, length = 100)
    private String cityName;

    @Size(max = 100, message = "State name must not exceed 100 characters")
    @Column(name = "state_name", length = 100)
    private String stateName;

    @NotBlank(message = "Country name is required")
    @Size(max = 100, message = "Country name must not exceed 100 characters")
    @Column(name = "country_name", nullable = false, length = 100)
    private String countryName;

    @Size(max = 20, message = "Postal code must not exceed 20 characters")
    @Column(name = "postal_code", length = 20)
    private String postalCode;

    @Size(max = 20, message = "Contact phone must not exceed 20 characters")
    @Column(name = "contact_phone", length = 20)
    private String contactPhone;

    @Email(message = "Contact email must be valid")
    @Size(max = 150, message = "Contact email must not exceed 150 characters")
    @Column(name = "contact_email", length = 150)
    private String contactEmail;

    @Min(value = 1, message = "Star rating must be at least 1")
    @Max(value = 5, message = "Star rating must not exceed 5")
    @Column(name = "star_rating")
    private Integer starRating;

    @Size(max = 500, message = "Cover image URL must not exceed 500 characters")
    @Column(name = "cover_image_url", length = 500)
    private String coverImageUrl;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "manager_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_hotel_manager"))
    private User manager;

    @Builder.Default
    @OneToMany(
            mappedBy = "hotel",
            cascade = CascadeType.ALL,
            fetch = FetchType.LAZY,
            orphanRemoval = true)
    private List<Room> rooms = new ArrayList<>();

    @Builder.Default
    @OneToMany(
            mappedBy = "hotel",
            cascade = CascadeType.ALL,
            fetch = FetchType.LAZY,
            orphanRemoval = true)
    private List<HotelAmenity> amenities = new ArrayList<>();
}
