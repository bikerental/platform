package com.bikerental.platform.rental.auth.security;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Principal object stored in SecurityContext after successful JWT authentication.
 * Contains identity and role extracted from JWT claims.
 */
@Getter
@AllArgsConstructor
public class HotelPrincipal {

    private final Long hotelId;
    private final String hotelCode;
    private final String role;

    /**
     * Check if this principal has admin role.
     */
    public boolean isAdmin() {
        return "ROLE_ADMIN".equals(role);
    }
}
