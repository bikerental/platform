package com.bikerental.platform.rental.common.exception;

/**
 * Thrown when an operation conflicts with existing state (e.g., duplicate hotel code).
 */
public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}
