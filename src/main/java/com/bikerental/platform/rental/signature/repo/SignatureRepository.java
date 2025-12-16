package com.bikerental.platform.rental.signature.repo;

import com.bikerental.platform.rental.signature.model.Signature;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for Signature entities.
 * Signatures are immutable and created during rental creation.
 * All queries are hotel-scoped for multi-tenant security.
 */
@Repository
public interface SignatureRepository extends JpaRepository<Signature, Long> {

    /**
     * Find a signature by ID, scoped to a specific hotel.
     * Use this instead of findById() to ensure multi-tenant isolation.
     */
    Optional<Signature> findBySignatureIdAndHotelId(Long signatureId, Long hotelId);
}

