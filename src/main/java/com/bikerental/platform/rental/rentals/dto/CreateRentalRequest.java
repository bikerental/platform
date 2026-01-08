package com.bikerental.platform.rental.rentals.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class CreateRentalRequest {

    @NotEmpty(message = "At least one bike is required")
    private List<@NotBlank(message = "Bike number cannot be blank") String> bikeNumbers;

    @NotBlank(message = "Room number is required")
    private String roomNumber;

    private String bedNumber;

    @NotNull(message = "Return date/time is required")
    private Instant returnDateTime;

    @NotBlank(message = "T&C version is required")
    private String tncVersion;

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

