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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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

    /**
     * Get overview data for the current hotel.
     * Includes bike counts by status, rental counts, and active/overdue rentals list.
     */
    @Transactional(readOnly = true)
    public OverviewResponse getOverview() {
        Long hotelId = hotelContext.getCurrentHotelId();

        // Count bikes by status
        int bikesAvailable = countBikesByStatus(hotelId, Bike.BikeStatus.AVAILABLE);
        int bikesRented = countBikesByStatus(hotelId, Bike.BikeStatus.RENTED);
        int bikesOoo = countBikesByStatus(hotelId, Bike.BikeStatus.OOO);

        // Count rentals by status
        int rentalsActive = (int) rentalRepository.countByHotelIdAndStatus(hotelId, RentalStatus.ACTIVE);
        int rentalsOverdue = (int) rentalRepository.countByHotelIdAndStatus(hotelId, RentalStatus.OVERDUE);

        // Get active and overdue rentals, ordered by urgency
        List<ActiveRentalSummary> activeRentals = getActiveRentalsSummary(hotelId);

        return OverviewResponse.builder()
                .bikesAvailable(bikesAvailable)
                .bikesRented(bikesRented)
                .bikesOoo(bikesOoo)
                .rentalsActive(rentalsActive)
                .rentalsOverdue(rentalsOverdue)
                .activeRentals(activeRentals)
                .build();
    }

    /**
     * Count bikes by status for a hotel.
     */
    private int countBikesByStatus(Long hotelId, Bike.BikeStatus status) {
        return bikeRepository.findByHotelIdAndStatus(hotelId, status).size();
    }

    /**
     * Get summary of active and overdue rentals, sorted by urgency.
     * Overdue rentals appear first, then sorted by dueAt ascending.
     */
    private List<ActiveRentalSummary> getActiveRentalsSummary(Long hotelId) {
        List<RentalStatus> statuses = List.of(RentalStatus.ACTIVE, RentalStatus.OVERDUE);
        List<Rental> rentals = rentalRepository.findActiveAndOverdueOrderedByUrgency(hotelId, statuses);

        return rentals.stream()
                .map(this::toActiveRentalSummary)
                .toList();
    }

    /**
     * Convert a Rental entity to ActiveRentalSummary DTO.
     * Calculates bikesOut (RENTED items) and bikesTotal.
     */
    private ActiveRentalSummary toActiveRentalSummary(Rental rental) {
        int bikesTotal = rental.getItems().size();
        int bikesOut = (int) rental.getItems().stream()
                .filter(item -> item.getStatus() == RentalItemStatus.RENTED)
                .count();

        return ActiveRentalSummary.builder()
                .rentalId(rental.getRentalId())
                .roomNumber(rental.getRoomNumber())
                .bedNumber(rental.getBedNumber())
                .dueAt(rental.getDueAt())
                .status(rental.getStatus())
                .bikesOut(bikesOut)
                .bikesTotal(bikesTotal)
                .build();
    }
}

