package com.bikerental.platform.rental.rentals.dto;

import com.bikerental.platform.rental.rentals.model.RentalItemStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Detailed response DTO for rental items.
 * Includes returnedAt timestamp and lostReason.
 */
@Getter
@Setter
@NoArgsConstructor
public class RentalItemDetailResponse {

    private Long rentalItemId;
    private Long bikeId;
    private String bikeNumber;
    private String bikeType;
    private RentalItemStatus status;
    private Instant returnedAt;
    private String lostReason;

    public RentalItemDetailResponse(Long rentalItemId, Long bikeId, String bikeNumber,
                                    String bikeType, RentalItemStatus status,
                                    Instant returnedAt, String lostReason) {
        this.rentalItemId = rentalItemId;
        this.bikeId = bikeId;
        this.bikeNumber = bikeNumber;
        this.bikeType = bikeType;
        this.status = status;
        this.returnedAt = returnedAt;
        this.lostReason = lostReason;
    }
}

