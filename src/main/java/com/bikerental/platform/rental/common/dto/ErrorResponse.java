package com.bikerental.platform.rental.common.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Standard error response format for all API errors.
 * See api_endpoints.mdc for format specification.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {

    /**
     * Machine-readable error code (e.g., BIKE_NOT_FOUND, INVALID_CREDENTIALS)
     */
    private String error;

    /**
     * Human-readable error message
     */
    private String message;

    /**
     * Optional additional context (e.g., which bikes failed)
     */
    private Object details;

    /**
     * ISO 8601 UTC timestamp
     */
    private String timestamp;
}
