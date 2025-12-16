package com.bikerental.platform.rental.rentals.model;

/**
 * Status of a rental item (individual bike in a rental).
 * - RENTED: bike currently out
 * - RETURNED: bike returned to hotel
 * - LOST: bike marked lost
 */
public enum RentalItemStatus {
    RENTED,
    RETURNED,
    LOST
}

