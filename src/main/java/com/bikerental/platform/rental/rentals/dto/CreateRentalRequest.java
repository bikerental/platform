package com.bikerental.platform.rental.rentals.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.List;

/**
 * Request DTO for creating a new rental.
 * All validations are done at the boundary (controller level).
 */
@Getter
@Setter
@NoArgsConstructor
public class CreateRentalRequest {

    /**
     * List of bike numbers to rent. Must contain at least one bike.
     */
    @NotEmpty(message = "At least one bike is required")
    private List<@NotBlank(message = "Bike number cannot be blank") String> bikeNumbers;

    /**
     * Guest's room number. Required.
     */
    @NotBlank(message = "Room number is required")
    private String roomNumber;

    /**
     * Guest's bed number. Optional.
     */
    private String bedNumber;

    /**
     * Expected return date/time in UTC. Must be in the future.
     */
    @NotNull(message = "Return date/time is required")
    private Instant returnDateTime;

    /**
     * T&C version the guest agreed to.
     */
    @NotBlank(message = "T&C version is required")
    private String tncVersion;

    /**
     * Guest's signature as base64-encoded PNG.
     * May include data URL prefix (data:image/png;base64,...)
     */
    @NotBlank(message = "Signature is required")
    private String signatureBase64Png;

    public CreateRentalRequest(List<String> bikeNumbers, String roomNumber, String bedNumber,
                               Instant returnDateTime, String tncVersion, String signatureBase64Png) {
        this.bikeNumbers = bikeNumbers;
        this.roomNumber = roomNumber;
        this.bedNumber = bedNumber;
        this.returnDateTime = returnDateTime;
        this.tncVersion = tncVersion;
        this.signatureBase64Png = signatureBase64Png;
    }
}

