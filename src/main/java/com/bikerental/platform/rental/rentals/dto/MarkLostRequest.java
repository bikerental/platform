package com.bikerental.platform.rental.rentals.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request DTO for marking a rental item as lost.
 */
@Getter
@Setter
@NoArgsConstructor
public class MarkLostRequest {

    /**
     * Optional reason/notes for why the item was marked as lost.
     */
    private String reason;

    public MarkLostRequest(String reason) {
        this.reason = reason;
    }
}

