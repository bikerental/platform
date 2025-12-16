package com.bikerental.platform.rental.bike.controller;

import com.bikerental.platform.rental.bike.dto.BikeResponse;
import com.bikerental.platform.rental.bike.dto.MarkOooRequest;
import com.bikerental.platform.rental.bike.model.Bike;
import com.bikerental.platform.rental.bike.service.BikeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/bikes")
@RequiredArgsConstructor
public class BikeController {

    private final BikeService bikeService;

    /**
     * List bikes for the current hotel.
     * Query parameters:
     * - status: optional filter by status (AVAILABLE, RENTED, OOO)
     * - q: optional search query (matches bike number)
     */
    @GetMapping
    public ResponseEntity<List<BikeResponse>> listBikes(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String q) {
        
        Bike.BikeStatus statusEnum = null;
        if (status != null && !status.isEmpty()) {
            try {
                statusEnum = Bike.BikeStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
                // Invalid status - return empty list or handle error
                return ResponseEntity.badRequest().build();
            }
        }
        
        List<Bike> bikes = bikeService.listBikes(statusEnum, q);
        List<BikeResponse> responses = bikes.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(responses);
    }

    /**
     * Get a bike by bike number.
     */
    @GetMapping("/by-number/{bikeNumber}")
    public ResponseEntity<BikeResponse> getBikeByNumber(@PathVariable String bikeNumber) {
        Bike bike = bikeService.findByBikeNumber(bikeNumber);
        return ResponseEntity.ok(toResponse(bike));
    }

    /**
     * Mark a bike as Out of Order (OOO).
     */
    @PatchMapping("/{bikeId}/ooo")
    public ResponseEntity<BikeResponse> markOoo(
            @PathVariable Long bikeId,
            @Valid @RequestBody MarkOooRequest request) {
        Bike bike = bikeService.markOoo(bikeId, request.getNote());
        return ResponseEntity.ok(toResponse(bike));
    }

    /**
     * Mark a bike as available.
     * Fails if bike is currently RENTED.
     */
    @PatchMapping("/{bikeId}/available")
    public ResponseEntity<BikeResponse> markAvailable(@PathVariable Long bikeId) {
        Bike bike = bikeService.markAvailable(bikeId);
        return ResponseEntity.ok(toResponse(bike));
    }

    private BikeResponse toResponse(Bike bike) {
        return BikeResponse.builder()
                .bikeId(bike.getBikeId())
                .bikeNumber(bike.getBikeNumber())
                .bikeType(bike.getBikeType())
                .status(bike.getStatus())
                .oooNote(bike.getOooNote())
                .oooSince(bike.getOooSince())
                .build();
    }
}

