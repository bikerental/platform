package com.bikerental.platform.rental.bike.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "bikes", uniqueConstraints = {
    @UniqueConstraint(name = "uk_hotel_bike_number", columnNames = {"hotel_id", "bike_number"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Bike {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "bike_id")
    private Long bikeId;

    @Column(name = "hotel_id", nullable = false)
    private Long hotelId;

    @Column(name = "bike_number", nullable = false, length = 50)
    private String bikeNumber;

    @Column(name = "bike_type", length = 50)
    private String bikeType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private BikeStatus status = BikeStatus.AVAILABLE;

    @Column(name = "ooo_note", columnDefinition = "TEXT")
    private String oooNote;

    @Column(name = "ooo_since")
    private Instant oooSince;

    public enum BikeStatus {
        AVAILABLE,
        RENTED,
        OOO
    }
}

