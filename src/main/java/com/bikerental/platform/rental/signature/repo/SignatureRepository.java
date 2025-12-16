package com.bikerental.platform.rental.signature.repo;

import com.bikerental.platform.rental.signature.model.Signature;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for Signature entities.
 * Signatures are immutable and created during rental creation.
 */
@Repository
public interface SignatureRepository extends JpaRepository<Signature, Long> {
}

