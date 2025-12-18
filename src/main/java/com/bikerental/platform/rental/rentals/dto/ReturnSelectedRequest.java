package com.bikerental.platform.rental.rentals.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Request DTO for returning multiple bikes at once.
 */
@Getter
@Setter
@NoArgsConstructor
public class ReturnSelectedRequest {

    @NotEmpty(message = "At least one rental item ID is required")
    private List<Long> rentalItemIds;

    public ReturnSelectedRequest(List<Long> rentalItemIds) {
        this.rentalItemIds = rentalItemIds;
    }
}

