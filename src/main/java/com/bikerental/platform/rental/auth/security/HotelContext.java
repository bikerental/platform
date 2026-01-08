package com.bikerental.platform.rental.auth.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Helper to extract hotel context from SecurityContext.
 * Use this in services to get the current hotel ID for scoping queries.
 * Never trust client-sent hotel IDs - always derive from JWT via this helper.
 */
@Component
public class HotelContext {

    public Long getCurrentHotelId() {
        return getCurrentPrincipal().getHotelId();
    }

    public String getCurrentHotelCode() {
        return getCurrentPrincipal().getHotelCode();
    }

    /** Extracts principal from Spring SecurityContext, throws if not authenticated or wrong type. */
    public HotelPrincipal getCurrentPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("No authenticated hotel in security context");
        }

        Object principal = authentication.getPrincipal();

        if (!(principal instanceof HotelPrincipal)) {
            String principalType = principal == null ? "null" : principal.getClass().getName();
            throw new IllegalStateException("Unexpected principal type: " + principalType);
        }

        return (HotelPrincipal) principal;
    }

    public boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null
                && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof HotelPrincipal;
    }
}
