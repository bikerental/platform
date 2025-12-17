package com.bikerental.platform.rental.common.exception;

import lombok.Getter;

import java.util.List;

/**
 * Thrown when one or more bikes are unavailable for rental.
 * Contains detailed information about which bikes are unavailable and why.
 */
@Getter
public class BikeUnavailableException extends RuntimeException {

    private final List<UnavailableBike> unavailableBikes;

    public BikeUnavailableException(String message, List<UnavailableBike> unavailableBikes) {
        super(message);
        this.unavailableBikes = unavailableBikes;
    }

    /**
     * Details about a single unavailable bike.
     */
    @Getter
    public static class UnavailableBike {
        private final String bikeNumber;
        private final String reason;

        public UnavailableBike(String bikeNumber, String reason) {
            this.bikeNumber = bikeNumber;
            this.reason = reason;
        }
    }
}

