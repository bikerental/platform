package com.bikerental.platform.rental.overview.dto;

import com.bikerental.platform.rental.rentals.model.RentalStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;

/**
 * Summary of an active or overdue rental for the overview display.
 */
@Getter
@Builder
public class ActiveRentalSummary {

    private final Long rentalId;
    private final String roomNumber;
    private final String bedNumber;
    private final Instant dueAt;
    private final RentalStatus status;
    private final int bikesOut;
    private final int bikesTotal;
    private final List<String> bikeNumbers;
}

