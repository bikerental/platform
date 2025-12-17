package com.bikerental.platform.rental.rentals.controller;

import com.bikerental.platform.rental.rentals.dto.CreateRentalRequest;
import com.bikerental.platform.rental.rentals.dto.RentalResponse;
import com.bikerental.platform.rental.rentals.service.RentalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
}

