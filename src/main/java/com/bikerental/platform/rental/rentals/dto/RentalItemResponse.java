package com.bikerental.platform.rental.rentals.dto;

import com.bikerental.platform.rental.rentals.model.RentalItemStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Response DTO for rental items.
 */
@Getter
@Setter
@NoArgsConstructor
public class RentalItemResponse {

    private Long rentalItemId;
    private Long bikeId;
    private String bikeNumber;
    private String bikeType;
    private RentalItemStatus status;

    public RentalItemResponse(Long rentalItemId, Long bikeId, String bikeNumber, 
                              String bikeType, RentalItemStatus status) {
        this.rentalItemId = rentalItemId;
        this.bikeId = bikeId;
        this.bikeNumber = bikeNumber;
        this.bikeType = bikeType;
        this.status = status;
    }
}

