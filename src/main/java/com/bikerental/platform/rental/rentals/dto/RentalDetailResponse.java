package com.bikerental.platform.rental.rentals.dto;

import com.bikerental.platform.rental.rentals.model.RentalStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.List;

/**
 * Detailed response DTO for rental retrieval.
 * Includes full item details with returnedAt and lostReason.
 */
@Getter
@Setter
@NoArgsConstructor
public class RentalDetailResponse {

    private Long rentalId;
    private RentalStatus status;
    private Instant startAt;
    private Instant dueAt;
    private Instant returnAt;
    private String roomNumber;
    private String bedNumber;
    private String tncVersion;
    private Long signatureId;
    private List<RentalItemDetailResponse> items;

    public RentalDetailResponse(Long rentalId, RentalStatus status, Instant startAt, Instant dueAt,
                                Instant returnAt, String roomNumber, String bedNumber, String tncVersion,
                                Long signatureId, List<RentalItemDetailResponse> items) {
        this.rentalId = rentalId;
        this.status = status;
        this.startAt = startAt;
        this.dueAt = dueAt;
        this.returnAt = returnAt;
        this.roomNumber = roomNumber;
        this.bedNumber = bedNumber;
        this.tncVersion = tncVersion;
        this.signatureId = signatureId;
        this.items = items;
    }
}

