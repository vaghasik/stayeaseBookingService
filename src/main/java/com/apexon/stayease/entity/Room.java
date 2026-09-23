package com.apexon.stayease.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.apexon.stayease.enums.RoomType;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

/**
 * Room Entity
 * Represents a hotel room in the StayEase system.
 *
 */
@Entity
@Table(
        name = "rooms",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_hotel_room",
                        columnNames = {"hotel_id", "room_number"}),
        indexes = {
            @Index(name = "idx_rooms_hotel", columnList = "hotel_id"),
            @Index(name = "idx_rooms_type", columnList = "room_type")
        })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"hotel", "bookings"})
@EqualsAndHashCode(exclude = {"hotel", "bookings"})
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "room_id", updatable = false, nullable = false)
    private UUID roomId;

    @NotBlank(message = "Room number is required")
    @Size(max = 20, message = "Room number must not exceed 20 characters")
    @Column(name = "room_number", nullable = false, length = 20)
    private String roomNumber;

    @NotNull(message = "Room type is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "room_type", nullable = false, length = 30)
    private RoomType roomType;

    @Column(name = "room_description", columnDefinition = "TEXT")
    private String roomDescription;

    @NotNull(message = "Price per night is required")
    @DecimalMin(value = "0.01", message = "Price per night must be greater than 0")
    @Digits(
            integer = 8,
            fraction = 2,
            message = "Price must have at most 8 integer digits and 2 decimal places")
    @Column(name = "price_per_night", nullable = false, precision = 10, scale = 2)
    private BigDecimal pricePerNight;

    @NotNull(message = "Max occupancy is required")
    @Min(value = 1, message = "Max occupancy must be at least 1")
    @Column(name = "max_occupancy", nullable = false)
    private Integer maxOccupancy;

    @Size(max = 500, message = "Room image URL must not exceed 500 characters")
    @Column(name = "room_image_url", length = 500)
    private String roomImageUrl;

    @Builder.Default
    @Column(name = "is_available", nullable = false)
    private Boolean isAvailable = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "hotel_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_room_hotel"))
    private Hotel hotel;

    @Builder.Default
    @OneToMany(mappedBy = "room", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Booking> bookings = new ArrayList<>();
}
