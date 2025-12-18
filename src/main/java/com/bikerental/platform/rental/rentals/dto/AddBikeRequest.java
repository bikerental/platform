package com.bikerental.platform.rental.rentals.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request DTO for adding a bike to an existing rental.
 */
@Getter
@Setter
@NoArgsConstructor
public class AddBikeRequest {

    @NotBlank(message = "Bike number is required")
    private String bikeNumber;

    public AddBikeRequest(String bikeNumber) {
        this.bikeNumber = bikeNumber;
    }
}

