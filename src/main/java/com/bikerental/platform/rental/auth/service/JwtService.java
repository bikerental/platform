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

    public String generateToken(Long hotelId, String hotelCode) {
        return generateToken(hotelId, hotelCode, ROLE_HOTEL);
    }

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

    /** Validates signature and expiration, returns empty if token is invalid or expired. */
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

    public Long extractHotelId(Claims claims) {
        return Long.parseLong(claims.getSubject());
    }

    public String extractHotelCode(Claims claims) {
        return claims.get("hotelCode", String.class);
    }

    public String extractRole(Claims claims) {
        String role = claims.get("role", String.class);
        return role != null ? role : ROLE_HOTEL;
    }
}
