package com.bikerental.platform.rental.common.exception;

/**
 * Thrown when a requested resource is not found.
 */
public class NotFoundException extends RuntimeException {
    public NotFoundException(String message) {
        super(message);
    }
}
