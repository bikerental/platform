package com.bikerental.platform.rental.rentals.controller;

import com.bikerental.platform.rental.rentals.dto.CreateRentalRequest;
import com.bikerental.platform.rental.rentals.dto.RentalDetailResponse;
import com.bikerental.platform.rental.rentals.dto.RentalResponse;
import com.bikerental.platform.rental.rentals.service.RentalContractService;
import com.bikerental.platform.rental.rentals.service.RentalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for rental operations.
 * Thin controller - delegates all business logic to RentalService.
 */
@RestController
@RequestMapping("/api/rentals")
@RequiredArgsConstructor
public class RentalController {

    private final RentalService rentalService;
    private final RentalContractService rentalContractService;

    /**
     * Create a new rental with the given bikes.
     *
     * @param request The rental creation request
     * @return The created rental (201 Created)
     */
    @PostMapping
    public ResponseEntity<RentalResponse> createRental(@Valid @RequestBody CreateRentalRequest request) {
        RentalResponse response = rentalService.createRental(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get detailed rental information by ID.
     *
     * @param rentalId The rental ID
     * @return The detailed rental information
     */
    @GetMapping("/{rentalId}")
    public ResponseEntity<RentalDetailResponse> getRentalDetail(@PathVariable Long rentalId) {
        RentalDetailResponse response = rentalService.getRentalDetail(rentalId);
        return ResponseEntity.ok(response);
    }

    /**
     * Get the signature image for a rental.
     *
     * @param rentalId The rental ID
     * @return The signature as PNG image
     */
    @GetMapping("/{rentalId}/signature")
    public ResponseEntity<byte[]> getRentalSignature(@PathVariable Long rentalId) {
        byte[] signatureData = rentalContractService.getSignatureForRental(rentalId);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.IMAGE_PNG);
        headers.setContentLength(signatureData.length);
        
        return new ResponseEntity<>(signatureData, headers, HttpStatus.OK);
    }

    /**
     * Get the contract document for a rental.
     *
     * @param rentalId The rental ID
     * @return The contract as HTML document
     */
    @GetMapping(value = "/{rentalId}/contract", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> getRentalContract(@PathVariable Long rentalId) {
        String contractHtml = rentalContractService.generateContractHtml(rentalId);
        return ResponseEntity.ok(contractHtml);
    }
}

