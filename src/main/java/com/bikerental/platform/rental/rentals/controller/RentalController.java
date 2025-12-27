package com.bikerental.platform.rental.rentals.controller;

import com.bikerental.platform.rental.rentals.dto.AddBikeRequest;
import com.bikerental.platform.rental.rentals.dto.CreateRentalRequest;
import com.bikerental.platform.rental.rentals.dto.MarkLostRequest;
import com.bikerental.platform.rental.rentals.dto.MarkLostResponse;
import com.bikerental.platform.rental.rentals.dto.RentalDetailResponse;
import com.bikerental.platform.rental.rentals.dto.RentalItemDetailResponse;
import com.bikerental.platform.rental.rentals.dto.RentalResponse;
import com.bikerental.platform.rental.rentals.dto.ReturnAllResponse;
import com.bikerental.platform.rental.rentals.dto.ReturnBikeResponse;
import com.bikerental.platform.rental.rentals.dto.ReturnSelectedRequest;
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

    /**
     * Return a single bike from a rental.
     *
     * @param rentalId The rental ID
     * @param rentalItemId The rental item ID
     * @return The return confirmation with updated status
     */
    @PostMapping("/{rentalId}/items/{rentalItemId}/return")
    public ResponseEntity<ReturnBikeResponse> returnBike(
            @PathVariable Long rentalId,
            @PathVariable Long rentalItemId) {
        ReturnBikeResponse response = rentalService.returnBike(rentalId, rentalItemId);
        return ResponseEntity.ok(response);
    }

    /**
     * Undo a bike return (within undo time window).
     *
     * @param rentalId The rental ID
     * @param rentalItemId The rental item ID
     * @return The undo confirmation with updated status
     */
    @PostMapping("/{rentalId}/items/{rentalItemId}/undo-return")
    public ResponseEntity<ReturnBikeResponse> undoReturn(
            @PathVariable Long rentalId,
            @PathVariable Long rentalItemId) {
        ReturnBikeResponse response = rentalService.undoReturn(rentalId, rentalItemId);
        return ResponseEntity.ok(response);
    }

    /**
     * Mark a bike as lost in a rental.
     * Sets item status to LOST, sets bike status to OOO, and recalculates rental status.
     *
     * @param rentalId The rental ID
     * @param rentalItemId The rental item ID
     * @param request Optional request with lost reason
     * @return The mark lost confirmation with updated status
     */
    @PostMapping("/{rentalId}/items/{rentalItemId}/lost")
    public ResponseEntity<MarkLostResponse> markLost(
            @PathVariable Long rentalId,
            @PathVariable Long rentalItemId,
            @RequestBody(required = false) MarkLostRequest request) {
        String reason = request != null ? request.getReason() : null;
        MarkLostResponse response = rentalService.markLost(rentalId, rentalItemId, reason);
        return ResponseEntity.ok(response);
    }

    /**
     * Return selected bikes from a rental.
     *
     * @param rentalId The rental ID
     * @param request The request containing rental item IDs to return
     * @return The return confirmation with all returned items
     */
    @PostMapping("/{rentalId}/return-selected")
    public ResponseEntity<ReturnAllResponse> returnSelected(
            @PathVariable Long rentalId,
            @Valid @RequestBody ReturnSelectedRequest request) {
        ReturnAllResponse response = rentalService.returnSelected(rentalId, request.getRentalItemIds());
        return ResponseEntity.ok(response);
    }

    /**
     * Return all remaining rented bikes from a rental.
     *
     * @param rentalId The rental ID
     * @return The return confirmation with all returned items
     */
    @PostMapping("/{rentalId}/return-all")
    public ResponseEntity<ReturnAllResponse> returnAll(@PathVariable Long rentalId) {
        ReturnAllResponse response = rentalService.returnAll(rentalId);
        return ResponseEntity.ok(response);
    }

    /**
     * Add a bike to an existing rental.
     * Only allowed for ACTIVE or OVERDUE rentals.
     *
     * @param rentalId The rental ID
     * @param request The request containing the bike number to add
     * @return The newly created rental item (201 Created)
     */
    @PostMapping("/{rentalId}/add-bike")
    public ResponseEntity<RentalItemDetailResponse> addBike(
            @PathVariable Long rentalId,
            @Valid @RequestBody AddBikeRequest request) {
        RentalItemDetailResponse response = rentalService.addBikeToRental(rentalId, request.getBikeNumber());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}

