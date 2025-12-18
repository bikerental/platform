package com.bikerental.platform.rental.rentals.dto;

import com.bikerental.platform.rental.rentals.model.RentalItemStatus;
import com.bikerental.platform.rental.rentals.model.RentalStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Response DTO for bike return operations.
 * Returns updated item status and rental status.
 */
@Getter
@Setter
@NoArgsConstructor
public class ReturnBikeResponse {

    private Long rentalItemId;
    private Long bikeId;
    private String bikeNumber;
    private RentalItemStatus itemStatus;
    private Instant returnedAt;
    private RentalStatus rentalStatus;
    private boolean rentalClosed;

    public ReturnBikeResponse(Long rentalItemId, Long bikeId, String bikeNumber,
                              RentalItemStatus itemStatus, Instant returnedAt,
                              RentalStatus rentalStatus, boolean rentalClosed) {
        this.rentalItemId = rentalItemId;
        this.bikeId = bikeId;
        this.bikeNumber = bikeNumber;
        this.itemStatus = itemStatus;
        this.returnedAt = returnedAt;
        this.rentalStatus = rentalStatus;
        this.rentalClosed = rentalClosed;
    }
}

