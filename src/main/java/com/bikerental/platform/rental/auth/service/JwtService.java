package com.bikerental.platform.rental.auth.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Optional;

@Service
public class JwtService {

    public static final String ROLE_ADMIN = "ROLE_ADMIN";
    public static final String ROLE_HOTEL = "ROLE_HOTEL";

    private final SecretKey secretKey;
    private final long expirationHours;

    public JwtService(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration-hours}") long expirationHours) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationHours = expirationHours;
    }

    /**
     * Generate a JWT token for a hotel (ROLE_HOTEL).
     */
    public String generateToken(Long hotelId, String hotelCode) {
        return generateToken(hotelId, hotelCode, ROLE_HOTEL);
    }

    /**
     * Generate a JWT token for admin (ROLE_ADMIN).
     */
    public String generateAdminToken(String username) {
        Instant now = Instant.now();
        Instant expiry = now.plus(expirationHours, ChronoUnit.HOURS);

        return Jwts.builder()
                .subject("0") // Admin has no hotel ID
                .claim("hotelCode", username)
                .claim("role", ROLE_ADMIN)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(secretKey)
                .compact();
    }

    /**
     * Generate a JWT token with specified role.
     */
    public String generateToken(Long hotelId, String hotelCode, String role) {
        Instant now = Instant.now();
        Instant expiry = now.plus(expirationHours, ChronoUnit.HOURS);

        return Jwts.builder()
                .subject(hotelId.toString())
                .claim("hotelCode", hotelCode)
                .claim("role", role)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(secretKey)
                .compact();
    }

    /**
     * Validate token and extract claims.
     * @return Optional.empty() if token is invalid or expired
     */
    public Optional<Claims> validateAndExtractClaims(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return Optional.of(claims);
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    /**
     * Extract hotel ID from valid claims.
     */
    public Long extractHotelId(Claims claims) {
        return Long.parseLong(claims.getSubject());
    }

    /**
     * Extract hotel code from valid claims.
     */
    public String extractHotelCode(Claims claims) {
        return claims.get("hotelCode", String.class);
    }

    /**
     * Extract role from valid claims. Defaults to ROLE_HOTEL for backwards compatibility.
     */
    public String extractRole(Claims claims) {
        String role = claims.get("role", String.class);
        return role != null ? role : ROLE_HOTEL;
    }
}
