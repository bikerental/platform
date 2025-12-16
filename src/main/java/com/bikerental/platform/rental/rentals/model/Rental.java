package com.bikerental.platform.rental.rentals.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a rental contract containing one or more bikes.
 * Scoped to a hotel via hotelId.
 */
@Entity
@Table(name = "rentals", indexes = {
    @Index(name = "idx_rental_hotel_status", columnList = "hotel_id, status"),
    @Index(name = "idx_rental_due_at", columnList = "hotel_id, due_at")
})
@Getter
@Setter
@NoArgsConstructor
public class Rental {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "rental_id")
    private Long rentalId;

    @Column(name = "hotel_id", nullable = false)
    private Long hotelId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private RentalStatus status = RentalStatus.ACTIVE;

    @Column(name = "start_at", nullable = false)
    private Instant startAt;

    @Column(name = "due_at", nullable = false)
    private Instant dueAt;

    @Column(name = "return_at")
    private Instant returnAt;

    @Column(name = "room_number", nullable = false, length = 50)
    private String roomNumber;

    @Column(name = "bed_number", length = 50)
    private String bedNumber;

    @Column(name = "tnc_version", nullable = false, length = 50)
    private String tncVersion;

    @Column(name = "signature_id", nullable = false)
    private Long signatureId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "rental", cascade = {CascadeType.PERSIST, CascadeType.MERGE}, orphanRemoval = false)
    private List<RentalItem> items = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }

    /**
     * Adds a rental item to this rental.
     * Sets the bidirectional relationship.
     */
    public void addItem(RentalItem item) {
        items.add(item);
        item.setRental(this);
    }

    /**
     * Removes a rental item from this rental.
     * Clears the bidirectional relationship.
     */
    public void removeItem(RentalItem item) {
        items.remove(item);
        item.setRental(null);
    }
}

