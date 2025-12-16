package com.bikerental.platform.rental.signature.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Stores the guest's signature as a PNG image blob.
 * Immutable once created - tied to a specific rental contract.
 * Hotel-scoped for multi-tenant security.
 */
@Entity
@Table(name = "signatures", indexes = {
    @Index(name = "idx_signature_hotel", columnList = "hotel_id")
})
@Getter
@Setter
@NoArgsConstructor
public class Signature {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "signature_id")
    private Long signatureId;

    /**
     * Hotel ID for multi-tenant scoping.
     * Ensures signatures cannot be accessed across hotels.
     */
    @Column(name = "hotel_id", nullable = false)
    private Long hotelId;

    @Lob
    @Column(name = "signature_data", nullable = false, columnDefinition = "MEDIUMBLOB")
    private byte[] signatureData;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }

    public Signature(Long hotelId, byte[] signatureData) {
        this.hotelId = hotelId;
        this.signatureData = signatureData;
    }
}

