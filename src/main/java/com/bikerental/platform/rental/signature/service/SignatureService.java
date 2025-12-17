package com.bikerental.platform.rental.signature.service;

import com.bikerental.platform.rental.signature.model.Signature;
import com.bikerental.platform.rental.signature.repo.SignatureRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Base64;
import java.util.Optional;

/**
 * Service for storing and retrieving guest signatures.
 * Signatures are immutable once created - tied to a specific rental contract.
 */
@Service
@RequiredArgsConstructor
public class SignatureService {

    private static final String DATA_URL_PREFIX = "data:image/png;base64,";

    private final SignatureRepository signatureRepository;

    /**
     * Store a signature from base64-encoded PNG data.
     * Handles data URL format (data:image/png;base64,...) by stripping prefix.
     *
     * @param hotelId The hotel ID for multi-tenant scoping
     * @param base64Png Base64-encoded PNG data (with or without data URL prefix)
     * @return The generated signature ID
     * @throws IllegalArgumentException if signature data is empty or invalid
     */
    @Transactional
    public Long storeSignature(Long hotelId, String base64Png) {
        if (base64Png == null || base64Png.isBlank()) {
            throw new IllegalArgumentException("Signature data cannot be empty");
        }

        // Strip data URL prefix if present
        String pureBase64 = base64Png.startsWith(DATA_URL_PREFIX)
                ? base64Png.substring(DATA_URL_PREFIX.length())
                : base64Png;

        byte[] signatureBytes;
        try {
            signatureBytes = Base64.getDecoder().decode(pureBase64);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid base64 signature data", e);
        }

        Signature signature = new Signature(hotelId, signatureBytes);
        Signature saved = signatureRepository.save(signature);
        
        return saved.getSignatureId();
    }

    /**
     * Retrieve a signature by ID, scoped to a specific hotel.
     *
     * @param signatureId The signature ID
     * @param hotelId The hotel ID for multi-tenant scoping
     * @return Optional containing the signature if found and belongs to hotel
     */
    public Optional<Signature> getSignature(Long signatureId, Long hotelId) {
        return signatureRepository.findBySignatureIdAndHotelId(signatureId, hotelId);
    }
}

