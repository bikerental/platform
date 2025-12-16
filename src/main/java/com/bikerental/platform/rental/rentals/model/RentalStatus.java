package com.bikerental.platform.rental.rentals.model;

/**
 * Status of a rental contract.
 * - ACTIVE: rental in progress, not yet overdue
 * - OVERDUE: rental has items RENTED past due_at + grace
 * - CLOSED: all items returned or lost
 */
public enum RentalStatus {
    ACTIVE,
    OVERDUE,
    CLOSED
}

