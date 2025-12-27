package com.bikerental.platform.rental.rentals.dto;

import com.bikerental.platform.rental.rentals.model.RentalItemStatus;
import com.bikerental.platform.rental.rentals.model.RentalStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Response DTO for marking a rental item as lost.
 */
@Getter
@Setter
@NoArgsConstructor
public class MarkLostResponse {

    private Long rentalItemId;
    private Long bikeId;
    private String bikeNumber;
    private RentalItemStatus itemStatus;
    private String lostReason;
    private RentalStatus rentalStatus;
    private boolean rentalClosed;

    public MarkLostResponse(Long rentalItemId, Long bikeId, String bikeNumber,
                            RentalItemStatus itemStatus, String lostReason,
                            RentalStatus rentalStatus, boolean rentalClosed) {
        this.rentalItemId = rentalItemId;
        this.bikeId = bikeId;
        this.bikeNumber = bikeNumber;
        this.itemStatus = itemStatus;
        this.lostReason = lostReason;
        this.rentalStatus = rentalStatus;
        this.rentalClosed = rentalClosed;
    }
}

