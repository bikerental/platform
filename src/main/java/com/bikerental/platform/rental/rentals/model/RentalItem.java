package com.bikerental.platform.rental.rentals.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Represents an individual bike in a rental contract.
 * Each rental item tracks the status of one bike: RENTED, RETURNED, or LOST.
 */
@Entity
@Table(name = "rental_items", indexes = {
    @Index(name = "idx_rental_item_bike", columnList = "bike_id")
})
@Getter
@Setter
@NoArgsConstructor
public class RentalItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "rental_item_id")
    private Long rentalItemId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rental_id", nullable = false)
    private Rental rental;

    @Column(name = "bike_id", nullable = false)
    private Long bikeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private RentalItemStatus status = RentalItemStatus.RENTED;

    @Column(name = "returned_at")
    private Instant returnedAt;

    @Column(name = "lost_reason", columnDefinition = "TEXT")
    private String lostReason;

    /**
     * Generated column for I1 invariant enforcement.
     * Equals bike_id when status = 'RENTED', NULL otherwise.
     * This column is managed by the database (generated column), read-only in JPA.
     */
    @Column(name = "rented_bike_id_if_rented", insertable = false, updatable = false)
    private Long rentedBikeIdIfRented;

    public RentalItem(Rental rental, Long bikeId) {
        this.rental = rental;
        this.bikeId = bikeId;
        this.status = RentalItemStatus.RENTED;
    }
}

