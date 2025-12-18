package com.bikerental.platform.rental.rentals.dto;

import com.bikerental.platform.rental.rentals.model.RentalStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.List;

/**
 * Response DTO for return-all and return-selected operations.
 */
@Getter
@Setter
@NoArgsConstructor
public class ReturnAllResponse {

    private Long rentalId;
    private RentalStatus rentalStatus;
    private Instant returnAt;
    private int returnedCount;
    private List<ReturnBikeResponse> returnedItems;

    public ReturnAllResponse(Long rentalId, RentalStatus rentalStatus, Instant returnAt,
                             int returnedCount, List<ReturnBikeResponse> returnedItems) {
        this.rentalId = rentalId;
        this.rentalStatus = rentalStatus;
        this.returnAt = returnAt;
        this.returnedCount = returnedCount;
        this.returnedItems = returnedItems;
    }
}

