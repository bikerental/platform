package com.bikerental.platform.rental.overview.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * Response DTO for the overview endpoint.
 * Contains bike counts, rental counts, and active/overdue rentals list.
 */
@Getter
@Builder
public class OverviewResponse {

    // Bike counts by status
    private final int bikesAvailable;
    private final int bikesRented;
    private final int bikesOoo;

    // Rental counts by status
    private final int rentalsActive;
    private final int rentalsOverdue;

    // Active and overdue rentals, sorted by urgency (overdue first, then by dueAt)
    private final List<ActiveRentalSummary> activeRentals;
}

