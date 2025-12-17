package com.bikerental.platform.rental.rentals.dto;

import com.bikerental.platform.rental.rentals.model.RentalStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.List;

/**
 * Response DTO for rental creation and retrieval.
 */
@Getter
@Setter
@NoArgsConstructor
public class RentalResponse {

    private Long rentalId;
    private RentalStatus status;
    private Instant startAt;
    private Instant dueAt;
    private String roomNumber;
    private String bedNumber;
    private List<RentalItemResponse> items;

    public RentalResponse(Long rentalId, RentalStatus status, Instant startAt, Instant dueAt,
                          String roomNumber, String bedNumber, List<RentalItemResponse> items) {
        this.rentalId = rentalId;
        this.status = status;
        this.startAt = startAt;
        this.dueAt = dueAt;
        this.roomNumber = roomNumber;
        this.bedNumber = bedNumber;
        this.items = items;
    }
}

