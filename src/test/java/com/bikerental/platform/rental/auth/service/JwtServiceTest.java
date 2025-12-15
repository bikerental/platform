package com.bikerental.platform.rental.auth.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String SECRET_KEY = "test-secret-key-that-is-at-least-256-bits-long-for-hmac-sha-256-algorithm";
    private static final long EXPIRATION_HOURS = 10;
    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(SECRET_KEY, EXPIRATION_HOURS);
    }

    @Test
    void generateToken_ForHotel_CreatesValidToken() {
        // Arrange
        Long hotelId = 1L;
        String hotelCode = "HOTEL001";

        // Act
        String token = jwtService.generateToken(hotelId, hotelCode);

        // Assert
        assertThat(token).isNotNull();
        assertThat(token.split("\\.")).hasSize(3); // JWT has 3 parts

        // Validate and extract claims
        Optional<Claims> claimsOpt = jwtService.validateAndExtractClaims(token);
        assertThat(claimsOpt).isPresent();

        Claims claims = claimsOpt.get();
        assertThat(jwtService.extractHotelId(claims)).isEqualTo(hotelId);
        assertThat(jwtService.extractHotelCode(claims)).isEqualTo(hotelCode);
        assertThat(jwtService.extractRole(claims)).isEqualTo(JwtService.ROLE_HOTEL);
    }

    @Test
    void generateAdminToken_CreatesValidAdminToken() {
        // Arrange
        String username = "admin";

        // Act
        String token = jwtService.generateAdminToken(username);

        // Assert
        assertThat(token).isNotNull();
        assertThat(token.split("\\.")).hasSize(3);

        Optional<Claims> claimsOpt = jwtService.validateAndExtractClaims(token);
        assertThat(claimsOpt).isPresent();

        Claims claims = claimsOpt.get();
        assertThat(jwtService.extractHotelId(claims)).isEqualTo(0L); // Admin has hotel ID 0
        assertThat(jwtService.extractHotelCode(claims)).isEqualTo(username);
        assertThat(jwtService.extractRole(claims)).isEqualTo(JwtService.ROLE_ADMIN);
    }

    @Test
    void validateAndExtractClaims_WithValidToken_ReturnsClaims() {
        // Arrange
        Long hotelId = 1L;
        String hotelCode = "HOTEL001";
        String token = jwtService.generateToken(hotelId, hotelCode);

        // Act
        Optional<Claims> claimsOpt = jwtService.validateAndExtractClaims(token);

        // Assert
        assertThat(claimsOpt).isPresent();
        Claims claims = claimsOpt.get();
        assertThat(claims.getSubject()).isEqualTo(hotelId.toString());
        assertThat(claims.get("hotelCode", String.class)).isEqualTo(hotelCode);
    }

    @Test
    void validateAndExtractClaims_WithInvalidToken_ReturnsEmpty() {
        // Arrange
        String invalidToken = "invalid.token.here";

        // Act
        Optional<Claims> claimsOpt = jwtService.validateAndExtractClaims(invalidToken);

        // Assert
        assertThat(claimsOpt).isEmpty();
    }

    @Test
    void validateAndExtractClaims_WithMalformedToken_ReturnsEmpty() {
        // Arrange
        String malformedToken = "not.a.jwt.token";

        // Act
        Optional<Claims> claimsOpt = jwtService.validateAndExtractClaims(malformedToken);

        // Assert
        assertThat(claimsOpt).isEmpty();
    }

    @Test
    void validateAndExtractClaims_WithTokenSignedWithDifferentKey_ReturnsEmpty() {
        // Arrange
        JwtService otherService = new JwtService("different-secret-key-that-is-at-least-256-bits-long-for-hmac-sha-256", EXPIRATION_HOURS);
        String token = otherService.generateToken(1L, "HOTEL001");

        // Act
        Optional<Claims> claimsOpt = jwtService.validateAndExtractClaims(token);

        // Assert
        assertThat(claimsOpt).isEmpty();
    }

    @Test
    void extractHotelId_WithValidClaims_ReturnsHotelId() {
        // Arrange
        Long hotelId = 42L;
        String token = jwtService.generateToken(hotelId, "HOTEL001");
        Claims claims = jwtService.validateAndExtractClaims(token).orElseThrow();

        // Act
        Long extractedId = jwtService.extractHotelId(claims);

        // Assert
        assertThat(extractedId).isEqualTo(hotelId);
    }

    @Test
    void extractHotelCode_WithValidClaims_ReturnsHotelCode() {
        // Arrange
        String hotelCode = "HOTEL999";
        String token = jwtService.generateToken(1L, hotelCode);
        Claims claims = jwtService.validateAndExtractClaims(token).orElseThrow();

        // Act
        String extractedCode = jwtService.extractHotelCode(claims);

        // Assert
        assertThat(extractedCode).isEqualTo(hotelCode);
    }

    @Test
    void extractRole_WithHotelToken_ReturnsRoleHotel() {
        // Arrange
        String token = jwtService.generateToken(1L, "HOTEL001");
        Claims claims = jwtService.validateAndExtractClaims(token).orElseThrow();

        // Act
        String role = jwtService.extractRole(claims);

        // Assert
        assertThat(role).isEqualTo(JwtService.ROLE_HOTEL);
    }

    @Test
    void extractRole_WithAdminToken_ReturnsRoleAdmin() {
        // Arrange
        String token = jwtService.generateAdminToken("admin");
        Claims claims = jwtService.validateAndExtractClaims(token).orElseThrow();

        // Act
        String role = jwtService.extractRole(claims);

        // Assert
        assertThat(role).isEqualTo(JwtService.ROLE_ADMIN);
    }

    @Test
    void generateToken_IncludesExpirationTime() {
        // Arrange
        Long hotelId = 1L;
        String hotelCode = "HOTEL001";

        // Act
        String token = jwtService.generateToken(hotelId, hotelCode);
        Optional<Claims> claimsOpt = jwtService.validateAndExtractClaims(token);

        // Assert
        assertThat(claimsOpt).isPresent();
        Claims claims = claimsOpt.get();
        assertThat(claims.getExpiration()).isNotNull();
        assertThat(claims.getExpiration().toInstant()).isAfter(Instant.now());
    }
}
