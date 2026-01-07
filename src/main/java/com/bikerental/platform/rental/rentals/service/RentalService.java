package com.bikerental.platform.rental.rentals.service;

import com.bikerental.platform.rental.auth.security.HotelContext;
import com.bikerental.platform.rental.bike.model.Bike;
import com.bikerental.platform.rental.bike.repo.BikeRepository;
import com.bikerental.platform.rental.common.exception.BikeUnavailableException;
import com.bikerental.platform.rental.common.exception.NotFoundException;
import com.bikerental.platform.rental.rentals.dto.CreateRentalRequest;
import com.bikerental.platform.rental.rentals.dto.MarkLostResponse;
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
import com.bikerental.platform.rental.settings.service.HotelSettingsService;
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

@Service
@RequiredArgsConstructor
public class RentalService {

    private final RentalRepository rentalRepository;
    private final RentalItemRepository rentalItemRepository;
    private final BikeRepository bikeRepository;
    private final SignatureService signatureService;
    private final HotelContext hotelContext;
    private final HotelSettingsService hotelSettingsService;

    // Atomic rental creation - validates all bikes before making any changes
    @Transactional
    public RentalResponse createRental(CreateRentalRequest request) {
        Long hotelId = hotelContext.getCurrentHotelId();

        validateRequest(request);
        checkForDuplicates(request.getBikeNumbers());
        List<Bike> bikes = validateAndCollectBikes(hotelId, request.getBikeNumbers());

        Long signatureId = signatureService.storeSignature(hotelId, request.getSignatureBase64Png());

        Rental rental = new Rental();
        rental.setHotelId(hotelId);
        rental.setStatus(RentalStatus.ACTIVE);
        rental.setStartAt(Instant.now());
        rental.setDueAt(request.getReturnDateTime());
        rental.setRoomNumber(request.getRoomNumber());
        rental.setBedNumber(request.getBedNumber());
        rental.setTncVersion(request.getTncVersion());
        rental.setSignatureId(signatureId);

        for (Bike bike : bikes) {
            RentalItem item = new RentalItem(rental, bike.getBikeId());
            rental.addItem(item);
            bike.setStatus(Bike.BikeStatus.RENTED);
            bikeRepository.save(bike);
        }

        Rental savedRental = rentalRepository.save(rental);
        return toRentalResponse(savedRental, bikes);
    }

    private void validateRequest(CreateRentalRequest request) {
        if (request.getBikeNumbers() == null || request.getBikeNumbers().isEmpty()) {
            throw new IllegalArgumentException("At least one bike is required");
        }

        if (request.getReturnDateTime() == null || request.getReturnDateTime().isBefore(Instant.now())) {
            throw new IllegalArgumentException("Return date/time must be in the future");
        }
    }

    private void checkForDuplicates(List<String> bikeNumbers) {
        Set<String> seen = new HashSet<>();
        for (String bikeNumber : bikeNumbers) {
            if (!seen.add(bikeNumber)) {
                throw new IllegalArgumentException("Duplicate bike number: " + bikeNumber);
            }
        }
    }

    // Collects all validation errors before throwing - better UX than failing on first error
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

    @Transactional(readOnly = true)
    public RentalDetailResponse getRentalDetail(Long rentalId) {
        Long hotelId = hotelContext.getCurrentHotelId();

        Rental rental = rentalRepository.findByRentalIdAndHotelId(rentalId, hotelId)
                .orElseThrow(() -> new NotFoundException("Rental not found: " + rentalId));

        List<Long> bikeIds = rental.getItems().stream()
                .map(RentalItem::getBikeId)
                .collect(Collectors.toList());

        Map<Long, Bike> bikeMap = bikeRepository.findAllById(bikeIds).stream()
                .collect(Collectors.toMap(Bike::getBikeId, Function.identity()));

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

        Bike bike = bikeRepository.findById(item.getBikeId())
                .orElseThrow(() -> new NotFoundException("Bike not found: " + item.getBikeId()));

        Instant returnedAt = Instant.now();
        item.setStatus(RentalItemStatus.RETURNED);
        item.setReturnedAt(returnedAt);
        rentalItemRepository.save(item);

        if (bike.getStatus() == Bike.BikeStatus.RENTED) {
            bike.setStatus(Bike.BikeStatus.AVAILABLE);
            bikeRepository.save(bike);
        }

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

    @Transactional
    public MarkLostResponse markLost(Long rentalId, Long rentalItemId, String reason) {
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

        Bike bike = bikeRepository.findById(item.getBikeId())
                .orElseThrow(() -> new NotFoundException("Bike not found: " + item.getBikeId()));

        item.setStatus(RentalItemStatus.LOST);
        item.setLostReason(reason);
        rentalItemRepository.save(item);

        bike.setStatus(Bike.BikeStatus.OOO);
        bike.setOooNote("Marked lost from rental #" + rentalId + (reason != null ? ": " + reason : ""));
        bike.setOooSince(Instant.now());
        bikeRepository.save(bike);

        boolean rentalClosed = recalculateRentalStatus(rental);

        return new MarkLostResponse(
                item.getRentalItemId(),
                bike.getBikeId(),
                bike.getBikeNumber(),
                item.getStatus(),
                item.getLostReason(),
                rental.getStatus(),
                rentalClosed
        );
    }

    @Transactional
    public ReturnAllResponse returnSelected(Long rentalId, List<Long> rentalItemIds) {
        Long hotelId = hotelContext.getCurrentHotelId();

        Rental rental = rentalRepository.findByRentalIdAndHotelId(rentalId, hotelId)
                .orElseThrow(() -> new NotFoundException("Rental not found: " + rentalId));

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
                item.setStatus(RentalItemStatus.RETURNED);
                item.setReturnedAt(returnedAt);
                rentalItemRepository.save(item);

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

        recalculateRentalStatus(rental);

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

    @Transactional
    public ReturnAllResponse returnAll(Long rentalId) {
        Long hotelId = hotelContext.getCurrentHotelId();

        Rental rental = rentalRepository.findByRentalIdAndHotelId(rentalId, hotelId)
                .orElseThrow(() -> new NotFoundException("Rental not found: " + rentalId));

        List<Long> bikeIds = rental.getItems().stream()
                .map(RentalItem::getBikeId)
                .collect(Collectors.toList());
        Map<Long, Bike> bikeMap = bikeRepository.findAllById(bikeIds).stream()
                .collect(Collectors.toMap(Bike::getBikeId, Function.identity()));

        Instant returnedAt = Instant.now();
        List<ReturnBikeResponse> returnedItems = new ArrayList<>();

        for (RentalItem item : rental.getItems()) {
            if (item.getStatus() == RentalItemStatus.RENTED) {
                item.setStatus(RentalItemStatus.RETURNED);
                item.setReturnedAt(returnedAt);
                rentalItemRepository.save(item);

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

        recalculateRentalStatus(rental);

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

    // Status logic: CLOSED if all items done, OVERDUE if past grace period, else ACTIVE
    private boolean recalculateRentalStatus(Rental rental) {
        boolean allReturned = rental.getItems().stream()
                .allMatch(item -> item.getStatus() == RentalItemStatus.RETURNED
                               || item.getStatus() == RentalItemStatus.LOST);

        if (allReturned) {
            if (rental.getStatus() != RentalStatus.CLOSED) {
                rental.setStatus(RentalStatus.CLOSED);
                rental.setReturnAt(Instant.now());
                rentalRepository.save(rental);
                return true;
            }
            return false;
        }

        boolean hasRentedItems = rental.getItems().stream()
                .anyMatch(item -> item.getStatus() == RentalItemStatus.RENTED);

        if (hasRentedItems) {
            int graceMinutes = hotelSettingsService.getGraceMinutes(rental.getHotelId());
            Instant overdueThreshold = rental.getDueAt().plusSeconds(graceMinutes * 60L);
            boolean isOverdue = Instant.now().isAfter(overdueThreshold);

            RentalStatus newStatus = isOverdue ? RentalStatus.OVERDUE : RentalStatus.ACTIVE;
            if (rental.getStatus() != newStatus) {
                rental.setStatus(newStatus);
                rentalRepository.save(rental);
            }
        }

        return false;
    }

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

        Bike bike = bikeRepository.findById(item.getBikeId())
                .orElseThrow(() -> new NotFoundException("Bike not found: " + item.getBikeId()));

        item.setStatus(RentalItemStatus.RENTED);
        item.setReturnedAt(null);
        rentalItemRepository.save(item);

        if (bike.getStatus() == Bike.BikeStatus.AVAILABLE) {
            bike.setStatus(Bike.BikeStatus.RENTED);
            bikeRepository.save(bike);
        }

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

    @Transactional
    public RentalItemDetailResponse addBikeToRental(Long rentalId, String bikeNumber) {
        Long hotelId = hotelContext.getCurrentHotelId();

        Rental rental = rentalRepository.findByRentalIdAndHotelId(rentalId, hotelId)
                .orElseThrow(() -> new NotFoundException("Rental not found: " + rentalId));

        if (rental.getStatus() == RentalStatus.CLOSED) {
            throw new IllegalStateException("Cannot add bikes to a closed rental");
        }

        List<Long> existingBikeIds = rental.getItems().stream()
                .map(RentalItem::getBikeId)
                .collect(Collectors.toList());

        Map<Long, Bike> existingBikeMap = bikeRepository.findAllById(existingBikeIds).stream()
                .collect(Collectors.toMap(Bike::getBikeId, Function.identity()));

        boolean alreadyInRental = existingBikeMap.values().stream()
                .anyMatch(b -> b.getBikeNumber().equals(bikeNumber));

        if (alreadyInRental) {
            throw new IllegalArgumentException("Bike " + bikeNumber + " is already in this rental");
        }

        List<Bike> bikes = validateAndCollectBikes(hotelId, List.of(bikeNumber));
        Bike bike = bikes.get(0);

        RentalItem newItem = new RentalItem(rental, bike.getBikeId());
        rental.addItem(newItem);
        rentalItemRepository.save(newItem);

        bike.setStatus(Bike.BikeStatus.RENTED);
        bikeRepository.save(bike);

        return new RentalItemDetailResponse(
                newItem.getRentalItemId(),
                bike.getBikeId(),
                bike.getBikeNumber(),
                bike.getBikeType(),
                newItem.getStatus(),
                null,
                null
        );
    }
}

