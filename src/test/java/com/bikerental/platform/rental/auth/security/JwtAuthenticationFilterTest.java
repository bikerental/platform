package com.bikerental.platform.rental.auth.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Optional;

import javax.crypto.SecretKey;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.bikerental.platform.rental.auth.service.JwtService;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class JwtAuthenticationFilterTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private static final String SECRET_KEY = "test-secret-key-that-is-at-least-256-bits-long-for-hmac-sha-256-algorithm";
    private static final Long HOTEL_ID = 1L;
    private static final String HOTEL_CODE = "HOTEL001";
    private static final String ROLE_HOTEL = "ROLE_HOTEL";

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void doFilterInternal_WithValidToken_SetsAuthentication() throws Exception {
        // Arrange
        String token = "valid.jwt.token";
        String authHeader = "Bearer " + token;

        Claims claims = createTestClaims(HOTEL_ID, HOTEL_CODE, ROLE_HOTEL);

        when(request.getHeader("Authorization")).thenReturn(authHeader);
        when(jwtService.validateAndExtractClaims(token)).thenReturn(Optional.of(claims));
        when(jwtService.extractHotelId(claims)).thenReturn(HOTEL_ID);
        when(jwtService.extractHotelCode(claims)).thenReturn(HOTEL_CODE);
        when(jwtService.extractRole(claims)).thenReturn(ROLE_HOTEL);

        // Act
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Assert
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.getPrincipal()).isInstanceOf(HotelPrincipal.class);
        assertThat(authentication.getAuthorities()).hasSize(1);
        assertThat(authentication.getAuthorities().iterator().next().getAuthority()).isEqualTo(ROLE_HOTEL);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_WithNoAuthorizationHeader_ContinuesFilterChain() throws Exception {
        // Arrange
        when(request.getHeader("Authorization")).thenReturn(null);

        // Act
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Assert
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNull();
        verify(jwtService, never()).validateAndExtractClaims(any());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_WithInvalidAuthHeaderFormat_ContinuesFilterChain() throws Exception {
        // Arrange
        when(request.getHeader("Authorization")).thenReturn("InvalidFormat token");

        // Act
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Assert
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNull();
        verify(jwtService, never()).validateAndExtractClaims(any());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_WithInvalidToken_ContinuesFilterChain() throws Exception {
        // Arrange
        String token = "invalid.token";
        String authHeader = "Bearer " + token;

        when(request.getHeader("Authorization")).thenReturn(authHeader);
        when(jwtService.validateAndExtractClaims(token)).thenReturn(Optional.empty());

        // Act
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Assert
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNull();
        verify(jwtService).validateAndExtractClaims(token);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_WithAdminToken_SetsAdminRole() throws Exception {
        // Arrange
        String token = "admin.jwt.token";
        String authHeader = "Bearer " + token;
        String roleAdmin = "ROLE_ADMIN";

        Claims claims = createTestClaims(0L, "admin", roleAdmin);

        when(request.getHeader("Authorization")).thenReturn(authHeader);
        when(jwtService.validateAndExtractClaims(token)).thenReturn(Optional.of(claims));
        when(jwtService.extractHotelId(claims)).thenReturn(0L);
        when(jwtService.extractHotelCode(claims)).thenReturn("admin");
        when(jwtService.extractRole(claims)).thenReturn(roleAdmin);

        // Act
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Assert
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.getAuthorities()).hasSize(1);
        assertThat(authentication.getAuthorities().iterator().next().getAuthority()).isEqualTo(roleAdmin);

        verify(filterChain).doFilter(request, response);
    }

    private Claims createTestClaims(Long hotelId, String hotelCode, String role) {
        SecretKey key = Keys.hmacShaKeyFor(SECRET_KEY.getBytes(StandardCharsets.UTF_8));
        Instant now = Instant.now();
        Instant expiry = now.plus(10, ChronoUnit.HOURS);

        String token = Jwts.builder()
                .subject(hotelId.toString())
                .claim("hotelCode", hotelCode)
                .claim("role", role)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(key)
                .compact();

        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
