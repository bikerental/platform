package com.bikerental.platform.rental.rentals.service;

import com.bikerental.platform.rental.auth.security.HotelContext;
import com.bikerental.platform.rental.bike.model.Bike;
import com.bikerental.platform.rental.bike.repo.BikeRepository;
import com.bikerental.platform.rental.common.exception.BikeUnavailableException;
import com.bikerental.platform.rental.rentals.dto.CreateRentalRequest;
import com.bikerental.platform.rental.rentals.dto.RentalItemResponse;
import com.bikerental.platform.rental.rentals.dto.RentalResponse;
import com.bikerental.platform.rental.rentals.model.Rental;
import com.bikerental.platform.rental.rentals.model.RentalItem;
import com.bikerental.platform.rental.rentals.model.RentalStatus;
import com.bikerental.platform.rental.rentals.repo.RentalRepository;
import com.bikerental.platform.rental.signature.service.SignatureService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Service for rental operations.
 * Handles rental creation with atomic transaction semantics.
 */
@Service
@RequiredArgsConstructor
public class RentalService {

    private final RentalRepository rentalRepository;
    private final BikeRepository bikeRepository;
    private final SignatureService signatureService;
    private final HotelContext hotelContext;

    /**
     * Create a new rental with the given bikes.
     * This is an atomic operation - all bikes are validated before any state is changed.
     *
     * @param request The rental creation request
     * @return The created rental response
     * @throws IllegalArgumentException if validation fails (empty bike list, past return time, duplicates)
     * @throws BikeUnavailableException if one or more bikes are unavailable (with details)
     */
    @Transactional
    public RentalResponse createRental(CreateRentalRequest request) {
        Long hotelId = hotelContext.getCurrentHotelId();

        // Validate request
        validateRequest(request);

        // Check for duplicates in the bike list
        checkForDuplicates(request.getBikeNumbers());

        // Validate all bikes exist and are available (collect all errors)
        List<Bike> bikes = validateAndCollectBikes(hotelId, request.getBikeNumbers());

        // Store signature
        Long signatureId = signatureService.storeSignature(hotelId, request.getSignatureBase64Png());

        // Create rental
        Rental rental = new Rental();
        rental.setHotelId(hotelId);
        rental.setStatus(RentalStatus.ACTIVE);
        rental.setStartAt(Instant.now());
        rental.setDueAt(request.getReturnDateTime());
        rental.setRoomNumber(request.getRoomNumber());
        rental.setBedNumber(request.getBedNumber());
        rental.setTncVersion(request.getTncVersion());
        rental.setSignatureId(signatureId);

        // Create rental items and update bike statuses
        for (Bike bike : bikes) {
            RentalItem item = new RentalItem(rental, bike.getBikeId());
            rental.addItem(item);

            // Update bike status to RENTED
            bike.setStatus(Bike.BikeStatus.RENTED);
            bikeRepository.save(bike);
        }

        // Save rental (cascades to items)
        Rental savedRental = rentalRepository.save(rental);

        // Convert to response
        return toRentalResponse(savedRental, bikes);
    }

    /**
     * Validate basic request constraints.
     */
    private void validateRequest(CreateRentalRequest request) {
        if (request.getBikeNumbers() == null || request.getBikeNumbers().isEmpty()) {
            throw new IllegalArgumentException("At least one bike is required");
        }

        if (request.getReturnDateTime() == null || request.getReturnDateTime().isBefore(Instant.now())) {
            throw new IllegalArgumentException("Return date/time must be in the future");
        }
    }

    /**
     * Check for duplicate bike numbers in the request.
     */
    private void checkForDuplicates(List<String> bikeNumbers) {
        Set<String> seen = new HashSet<>();
        for (String bikeNumber : bikeNumbers) {
            if (!seen.add(bikeNumber)) {
                throw new IllegalArgumentException("Duplicate bike number: " + bikeNumber);
            }
        }
    }

    /**
     * Validate all bikes exist and are available.
     * Collects all unavailable bikes before throwing exception.
     */
    private List<Bike> validateAndCollectBikes(Long hotelId, List<String> bikeNumbers) {
        List<Bike> bikes = new ArrayList<>();
        List<BikeUnavailableException.UnavailableBike> unavailableBikes = new ArrayList<>();

        for (String bikeNumber : bikeNumbers) {
            Optional<Bike> bikeOpt = bikeRepository.findByHotelIdAndBikeNumber(hotelId, bikeNumber);

            if (bikeOpt.isEmpty()) {
                unavailableBikes.add(new BikeUnavailableException.UnavailableBike(bikeNumber, "NOT_FOUND"));
            } else {
                Bike bike = bikeOpt.get();
                if (bike.getStatus() == Bike.BikeStatus.RENTED) {
                    unavailableBikes.add(new BikeUnavailableException.UnavailableBike(bikeNumber, "ALREADY_RENTED"));
                } else if (bike.getStatus() == Bike.BikeStatus.OOO) {
                    unavailableBikes.add(new BikeUnavailableException.UnavailableBike(bikeNumber, "OUT_OF_ORDER"));
                } else {
                    bikes.add(bike);
                }
            }
        }

        if (!unavailableBikes.isEmpty()) {
            throw new BikeUnavailableException(
                    "One or more bikes are unavailable",
                    unavailableBikes
            );
        }

        return bikes;
    }

    /**
     * Convert a Rental entity to a RentalResponse DTO.
     */
    private RentalResponse toRentalResponse(Rental rental, List<Bike> bikes) {
        List<RentalItemResponse> itemResponses = new ArrayList<>();

        for (int i = 0; i < rental.getItems().size(); i++) {
            RentalItem item = rental.getItems().get(i);
            Bike bike = bikes.get(i);

            itemResponses.add(new RentalItemResponse(
                    item.getRentalItemId(),
                    item.getBikeId(),
                    bike.getBikeNumber(),
                    bike.getBikeType(),
                    item.getStatus()
            ));
        }

        return new RentalResponse(
                rental.getRentalId(),
                rental.getStatus(),
                rental.getStartAt(),
                rental.getDueAt(),
                rental.getRoomNumber(),
                rental.getBedNumber(),
                itemResponses
        );
    }
}

