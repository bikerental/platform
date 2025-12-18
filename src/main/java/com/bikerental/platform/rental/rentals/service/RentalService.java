package com.bikerental.platform.rental.rentals.service;

import com.bikerental.platform.rental.auth.security.HotelContext;
import com.bikerental.platform.rental.bike.model.Bike;
import com.bikerental.platform.rental.bike.repo.BikeRepository;
import com.bikerental.platform.rental.common.exception.BikeUnavailableException;
import com.bikerental.platform.rental.common.exception.NotFoundException;
import com.bikerental.platform.rental.rentals.dto.CreateRentalRequest;
import com.bikerental.platform.rental.rentals.dto.RentalDetailResponse;
import com.bikerental.platform.rental.rentals.dto.RentalItemDetailResponse;
import com.bikerental.platform.rental.rentals.dto.RentalItemResponse;
import com.bikerental.platform.rental.rentals.dto.RentalResponse;
import com.bikerental.platform.rental.rentals.dto.ReturnAllResponse;
import com.bikerental.platform.rental.rentals.dto.ReturnBikeResponse;
import com.bikerental.platform.rental.rentals.model.Rental;
import com.bikerental.platform.rental.rentals.model.RentalItem;
import com.bikerental.platform.rental.rentals.model.RentalItemStatus;
import com.bikerental.platform.rental.rentals.model.RentalStatus;
import com.bikerental.platform.rental.rentals.repo.RentalItemRepository;
import com.bikerental.platform.rental.rentals.repo.RentalRepository;
import com.bikerental.platform.rental.signature.service.SignatureService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Service for rental operations.
 * Handles rental creation with atomic transaction semantics.
 */
@Service
@RequiredArgsConstructor
public class RentalService {

    private final RentalRepository rentalRepository;
    private final RentalItemRepository rentalItemRepository;
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

    /**
     * Get detailed rental information by ID.
     * Includes full item details with returnedAt and lostReason.
     *
     * @param rentalId The rental ID
     * @return The detailed rental response
     * @throws NotFoundException if rental not found or doesn't belong to hotel
     */
    @Transactional(readOnly = true)
    public RentalDetailResponse getRentalDetail(Long rentalId) {
        Long hotelId = hotelContext.getCurrentHotelId();

        Rental rental = rentalRepository.findByRentalIdAndHotelId(rentalId, hotelId)
                .orElseThrow(() -> new NotFoundException("Rental not found: " + rentalId));

        // Fetch all bikes for the rental items
        List<Long> bikeIds = rental.getItems().stream()
                .map(RentalItem::getBikeId)
                .collect(Collectors.toList());

        Map<Long, Bike> bikeMap = bikeRepository.findAllById(bikeIds).stream()
                .collect(Collectors.toMap(Bike::getBikeId, Function.identity()));

        // Convert items to detailed responses
        List<RentalItemDetailResponse> itemResponses = rental.getItems().stream()
                .map(item -> {
                    Bike bike = bikeMap.get(item.getBikeId());
                    return new RentalItemDetailResponse(
                            item.getRentalItemId(),
                            item.getBikeId(),
                            bike != null ? bike.getBikeNumber() : "Unknown",
                            bike != null ? bike.getBikeType() : null,
                            item.getStatus(),
                            item.getReturnedAt(),
                            item.getLostReason()
                    );
                })
                .collect(Collectors.toList());

        return new RentalDetailResponse(
                rental.getRentalId(),
                rental.getStatus(),
                rental.getStartAt(),
                rental.getDueAt(),
                rental.getReturnAt(),
                rental.getRoomNumber(),
                rental.getBedNumber(),
                rental.getTncVersion(),
                rental.getSignatureId(),
                itemResponses
        );
    }

    /**
     * Return a single bike from a rental.
     * Sets item status to RETURNED, updates bike status to AVAILABLE (unless OOO),
     * and recalculates rental status.
     *
     * @param rentalId The rental ID
     * @param rentalItemId The rental item ID
     * @return Response with updated item and rental status
     * @throws NotFoundException if rental or item not found
     * @throws IllegalStateException if item is not in RENTED status
     */
    @Transactional
    public ReturnBikeResponse returnBike(Long rentalId, Long rentalItemId) {
        Long hotelId = hotelContext.getCurrentHotelId();

        Rental rental = rentalRepository.findByRentalIdAndHotelId(rentalId, hotelId)
                .orElseThrow(() -> new NotFoundException("Rental not found: " + rentalId));

        RentalItem item = rental.getItems().stream()
                .filter(i -> i.getRentalItemId().equals(rentalItemId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Rental item not found: " + rentalItemId));

        if (item.getStatus() != RentalItemStatus.RENTED) {
            throw new IllegalStateException("Item is not currently rented");
        }

        // Get bike info for response
        Bike bike = bikeRepository.findById(item.getBikeId())
                .orElseThrow(() -> new NotFoundException("Bike not found: " + item.getBikeId()));

        // Return the item
        Instant returnedAt = Instant.now();
        item.setStatus(RentalItemStatus.RETURNED);
        item.setReturnedAt(returnedAt);
        rentalItemRepository.save(item);

        // Update bike status to AVAILABLE (unless it was marked OOO)
        if (bike.getStatus() == Bike.BikeStatus.RENTED) {
            bike.setStatus(Bike.BikeStatus.AVAILABLE);
            bikeRepository.save(bike);
        }

        // Recalculate rental status
        boolean rentalClosed = recalculateRentalStatus(rental);

        return new ReturnBikeResponse(
                item.getRentalItemId(),
                bike.getBikeId(),
                bike.getBikeNumber(),
                item.getStatus(),
                returnedAt,
                rental.getStatus(),
                rentalClosed
        );
    }

    /**
     * Return selected bikes from a rental.
     *
     * @param rentalId The rental ID
     * @param rentalItemIds List of rental item IDs to return
     * @return Response with all returned items and rental status
     */
    @Transactional
    public ReturnAllResponse returnSelected(Long rentalId, List<Long> rentalItemIds) {
        Long hotelId = hotelContext.getCurrentHotelId();

        Rental rental = rentalRepository.findByRentalIdAndHotelId(rentalId, hotelId)
                .orElseThrow(() -> new NotFoundException("Rental not found: " + rentalId));

        // Fetch all bikes for this rental
        List<Long> bikeIds = rental.getItems().stream()
                .map(RentalItem::getBikeId)
                .collect(Collectors.toList());
        Map<Long, Bike> bikeMap = bikeRepository.findAllById(bikeIds).stream()
                .collect(Collectors.toMap(Bike::getBikeId, Function.identity()));

        Instant returnedAt = Instant.now();
        List<ReturnBikeResponse> returnedItems = new ArrayList<>();
        Set<Long> itemIdSet = new HashSet<>(rentalItemIds);

        for (RentalItem item : rental.getItems()) {
            if (itemIdSet.contains(item.getRentalItemId()) && item.getStatus() == RentalItemStatus.RENTED) {
                // Return this item
                item.setStatus(RentalItemStatus.RETURNED);
                item.setReturnedAt(returnedAt);
                rentalItemRepository.save(item);

                // Update bike status
                Bike bike = bikeMap.get(item.getBikeId());
                if (bike != null && bike.getStatus() == Bike.BikeStatus.RENTED) {
                    bike.setStatus(Bike.BikeStatus.AVAILABLE);
                    bikeRepository.save(bike);
                }

                returnedItems.add(new ReturnBikeResponse(
                        item.getRentalItemId(),
                        bike != null ? bike.getBikeId() : item.getBikeId(),
                        bike != null ? bike.getBikeNumber() : "Unknown",
                        item.getStatus(),
                        returnedAt,
                        null, // Will be set after recalculation
                        false
                ));
            }
        }

        // Recalculate rental status
        recalculateRentalStatus(rental);

        // Update rental status in responses
        for (ReturnBikeResponse response : returnedItems) {
            response.setRentalStatus(rental.getStatus());
            response.setRentalClosed(rental.getStatus() == RentalStatus.CLOSED);
        }

        return new ReturnAllResponse(
                rental.getRentalId(),
                rental.getStatus(),
                rental.getReturnAt(),
                returnedItems.size(),
                returnedItems
        );
    }

    /**
     * Return all remaining rented bikes from a rental.
     *
     * @param rentalId The rental ID
     * @return Response with all returned items and rental status
     */
    @Transactional
    public ReturnAllResponse returnAll(Long rentalId) {
        Long hotelId = hotelContext.getCurrentHotelId();

        Rental rental = rentalRepository.findByRentalIdAndHotelId(rentalId, hotelId)
                .orElseThrow(() -> new NotFoundException("Rental not found: " + rentalId));

        // Fetch all bikes for this rental
        List<Long> bikeIds = rental.getItems().stream()
                .map(RentalItem::getBikeId)
                .collect(Collectors.toList());
        Map<Long, Bike> bikeMap = bikeRepository.findAllById(bikeIds).stream()
                .collect(Collectors.toMap(Bike::getBikeId, Function.identity()));

        Instant returnedAt = Instant.now();
        List<ReturnBikeResponse> returnedItems = new ArrayList<>();

        for (RentalItem item : rental.getItems()) {
            if (item.getStatus() == RentalItemStatus.RENTED) {
                // Return this item
                item.setStatus(RentalItemStatus.RETURNED);
                item.setReturnedAt(returnedAt);
                rentalItemRepository.save(item);

                // Update bike status
                Bike bike = bikeMap.get(item.getBikeId());
                if (bike != null && bike.getStatus() == Bike.BikeStatus.RENTED) {
                    bike.setStatus(Bike.BikeStatus.AVAILABLE);
                    bikeRepository.save(bike);
                }

                returnedItems.add(new ReturnBikeResponse(
                        item.getRentalItemId(),
                        bike != null ? bike.getBikeId() : item.getBikeId(),
                        bike != null ? bike.getBikeNumber() : "Unknown",
                        item.getStatus(),
                        returnedAt,
                        null,
                        false
                ));
            }
        }

        // Recalculate rental status (will always be CLOSED after return-all)
        recalculateRentalStatus(rental);

        // Update rental status in responses
        for (ReturnBikeResponse response : returnedItems) {
            response.setRentalStatus(rental.getStatus());
            response.setRentalClosed(rental.getStatus() == RentalStatus.CLOSED);
        }

        return new ReturnAllResponse(
                rental.getRentalId(),
                rental.getStatus(),
                rental.getReturnAt(),
                returnedItems.size(),
                returnedItems
        );
    }

    /**
     * Recalculate rental status based on item statuses.
     * If all items are RETURNED or LOST, set rental to CLOSED and set returnAt.
     *
     * @param rental The rental to recalculate
     * @return true if rental was closed, false otherwise
     */
    private boolean recalculateRentalStatus(Rental rental) {
        boolean allReturned = rental.getItems().stream()
                .allMatch(item -> item.getStatus() == RentalItemStatus.RETURNED 
                               || item.getStatus() == RentalItemStatus.LOST);

        if (allReturned && rental.getStatus() != RentalStatus.CLOSED) {
            rental.setStatus(RentalStatus.CLOSED);
            rental.setReturnAt(Instant.now());
            rentalRepository.save(rental);
            return true;
        }

        return false;
    }

    /**
     * Undo a bike return (for undo functionality within time window).
     * Sets item status back to RENTED, updates bike status back to RENTED.
     *
     * @param rentalId The rental ID
     * @param rentalItemId The rental item ID
     * @return Response with updated item and rental status
     * @throws NotFoundException if rental or item not found
     * @throws IllegalStateException if item is not in RETURNED status
     */
    @Transactional
    public ReturnBikeResponse undoReturn(Long rentalId, Long rentalItemId) {
        Long hotelId = hotelContext.getCurrentHotelId();

        Rental rental = rentalRepository.findByRentalIdAndHotelId(rentalId, hotelId)
                .orElseThrow(() -> new NotFoundException("Rental not found: " + rentalId));

        RentalItem item = rental.getItems().stream()
                .filter(i -> i.getRentalItemId().equals(rentalItemId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Rental item not found: " + rentalItemId));

        if (item.getStatus() != RentalItemStatus.RETURNED) {
            throw new IllegalStateException("Item is not in RETURNED status");
        }

        // Get bike info
        Bike bike = bikeRepository.findById(item.getBikeId())
                .orElseThrow(() -> new NotFoundException("Bike not found: " + item.getBikeId()));

        // Undo the return
        item.setStatus(RentalItemStatus.RENTED);
        item.setReturnedAt(null);
        rentalItemRepository.save(item);

        // Update bike status back to RENTED (unless it's OOO)
        if (bike.getStatus() == Bike.BikeStatus.AVAILABLE) {
            bike.setStatus(Bike.BikeStatus.RENTED);
            bikeRepository.save(bike);
        }

        // Recalculate rental status (reopen if was closed)
        if (rental.getStatus() == RentalStatus.CLOSED) {
            rental.setStatus(RentalStatus.ACTIVE);
            rental.setReturnAt(null);
            rentalRepository.save(rental);
        }

        return new ReturnBikeResponse(
                item.getRentalItemId(),
                bike.getBikeId(),
                bike.getBikeNumber(),
                item.getStatus(),
                null,
                rental.getStatus(),
                false
        );
    }
}

