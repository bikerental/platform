package com.bikerental.platform.rental.overview.service;

import com.bikerental.platform.rental.auth.security.HotelContext;
import com.bikerental.platform.rental.bike.model.Bike;
import com.bikerental.platform.rental.bike.repo.BikeRepository;
import com.bikerental.platform.rental.overview.dto.ActiveRentalSummary;
import com.bikerental.platform.rental.overview.dto.OverviewResponse;
import com.bikerental.platform.rental.rentals.model.Rental;
import com.bikerental.platform.rental.rentals.model.RentalItemStatus;
import com.bikerental.platform.rental.rentals.model.RentalStatus;
import com.bikerental.platform.rental.rentals.repo.RentalRepository;
import com.bikerental.platform.rental.settings.service.HotelSettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service for computing overview/dashboard data.
 * Aggregates bike and rental statistics for the current hotel.
 */
@Service
@RequiredArgsConstructor
public class OverviewService {

    private final BikeRepository bikeRepository;
    private final RentalRepository rentalRepository;
    private final HotelContext hotelContext;
    private final HotelSettingsService hotelSettingsService;

    /**
     * Get overview data for the current hotel.
     * Includes bike counts by status, rental counts, and active/overdue rentals list.
     * Overdue status is computed dynamically based on dueAt + grace period.
     */
    @Transactional(readOnly = true)
    public OverviewResponse getOverview() {
        Long hotelId = hotelContext.getCurrentHotelId();

        // Count bikes by status
        long bikesAvailable = bikeRepository.countByHotelIdAndStatus(hotelId, Bike.BikeStatus.AVAILABLE);
        long bikesRented = bikeRepository.countByHotelIdAndStatus(hotelId, Bike.BikeStatus.RENTED);
        long bikesOoo = bikeRepository.countByHotelIdAndStatus(hotelId, Bike.BikeStatus.OOO);

        // Get active and overdue rentals with dynamically computed status
        int graceMinutes = hotelSettingsService.getGraceMinutes(hotelId);
        Instant overdueThreshold = Instant.now().minusSeconds(graceMinutes * 60L);
        List<ActiveRentalSummary> activeRentals = getActiveRentalsSummary(hotelId, overdueThreshold);

        // Count rentals by computed status
        long rentalsActive = activeRentals.stream()
                .filter(r -> r.getStatus() == RentalStatus.ACTIVE)
                .count();
        long rentalsOverdue = activeRentals.stream()
                .filter(r -> r.getStatus() == RentalStatus.OVERDUE)
                .count();

        return OverviewResponse.builder()
                .bikesAvailable((int) bikesAvailable)
                .bikesRented((int) bikesRented)
                .bikesOoo((int) bikesOoo)
                .rentalsActive((int) rentalsActive)
                .rentalsOverdue((int) rentalsOverdue)
                .activeRentals(activeRentals)
                .build();
    }

    /**
     * Get summary of active and overdue rentals, sorted by urgency.
     * Overdue status is computed dynamically: overdue if dueAt < overdueThreshold.
     * Overdue rentals appear first, then sorted by dueAt ascending.
     */
    private List<ActiveRentalSummary> getActiveRentalsSummary(Long hotelId, Instant overdueThreshold) {
        List<RentalStatus> statuses = List.of(RentalStatus.ACTIVE, RentalStatus.OVERDUE);
        List<Rental> rentals = rentalRepository.findActiveAndOverdueOrderedByUrgency(
                hotelId, statuses, RentalStatus.OVERDUE);

        // Collect all bike IDs from rented items and fetch bike numbers in one query
        List<Long> allBikeIds = rentals.stream()
                .flatMap(r -> r.getItems().stream())
                .filter(item -> item.getStatus() == RentalItemStatus.RENTED)
                .map(item -> item.getBikeId())
                .distinct()
                .toList();

        Map<Long, String> bikeIdToNumber = bikeRepository.findAllById(allBikeIds).stream()
                .collect(Collectors.toMap(Bike::getBikeId, Bike::getBikeNumber));

        return rentals.stream()
                .map(rental -> toActiveRentalSummary(rental, overdueThreshold, bikeIdToNumber))
                .sorted((a, b) -> {
                    // Sort overdue first, then by dueAt ascending
                    if (a.getStatus() == RentalStatus.OVERDUE && b.getStatus() != RentalStatus.OVERDUE) {
                        return -1;
                    }
                    if (a.getStatus() != RentalStatus.OVERDUE && b.getStatus() == RentalStatus.OVERDUE) {
                        return 1;
                    }
                    return a.getDueAt().compareTo(b.getDueAt());
                })
                .toList();
    }

    /**
     * Convert a Rental entity to ActiveRentalSummary DTO.
     * Calculates bikesOut (RENTED items) and bikesTotal.
     * Computes overdue status dynamically based on overdueThreshold.
     */
    private ActiveRentalSummary toActiveRentalSummary(Rental rental, Instant overdueThreshold,
                                                       Map<Long, String> bikeIdToNumber) {
        List<Long> rentedBikeIds = rental.getItems().stream()
                .filter(item -> item.getStatus() == RentalItemStatus.RENTED)
                .map(item -> item.getBikeId())
                .toList();

        int bikesTotal = rental.getItems().size();
        int bikesOut = rentedBikeIds.size();

        List<String> bikeNumbers = rentedBikeIds.stream()
                .map(bikeIdToNumber::get)
                .filter(n -> n != null)
                .toList();

        // Compute status dynamically: overdue if dueAt is before the threshold
        RentalStatus computedStatus = rental.getDueAt().isBefore(overdueThreshold)
                ? RentalStatus.OVERDUE
                : RentalStatus.ACTIVE;

        return ActiveRentalSummary.builder()
                .rentalId(rental.getRentalId())
                .roomNumber(rental.getRoomNumber())
                .bedNumber(rental.getBedNumber())
                .dueAt(rental.getDueAt())
                .status(computedStatus)
                .bikesOut(bikesOut)
                .bikesTotal(bikesTotal)
                .bikeNumbers(bikeNumbers)
                .build();
    }
}

