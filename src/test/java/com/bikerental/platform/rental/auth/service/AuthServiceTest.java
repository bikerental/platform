package com.bikerental.platform.rental.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.bikerental.platform.rental.auth.dto.LoginRequest;
import com.bikerental.platform.rental.auth.dto.LoginResponse;
import com.bikerental.platform.rental.auth.model.Hotel;
import com.bikerental.platform.rental.auth.repo.HotelRepository;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private HotelRepository hotelRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    private AuthService authService;

    private static final String ADMIN_CODE = "admin";
    private static final String HOTEL_CODE = "HOTEL001";
    private static final String HOTEL_PASSWORD = "hotelPassword123";
    private static final String HOTEL_NAME = "Test Hotel";
    private static final Long HOTEL_ID = 1L;
    private static final String JWT_TOKEN = "test.jwt.token";

    @BeforeEach
    void setUp() {
        authService = new AuthService(hotelRepository, passwordEncoder, jwtService);
    }

    @Test
    void authenticate_WithValidHotelCredentials_ReturnsLoginResponse() {
        // Arrange
        LoginRequest request = new LoginRequest(HOTEL_CODE, HOTEL_PASSWORD);
        Hotel hotel = new Hotel();
        hotel.setHotelId(HOTEL_ID);
        hotel.setHotelCode(HOTEL_CODE);
        hotel.setHotelName(HOTEL_NAME);
        hotel.setPasswordHash("hashedPassword");
        hotel.setAdmin(false);

        when(hotelRepository.findByHotelCode(HOTEL_CODE)).thenReturn(Optional.of(hotel));
        when(passwordEncoder.matches(HOTEL_PASSWORD, hotel.getPasswordHash())).thenReturn(true);
        when(jwtService.generateToken(HOTEL_ID, HOTEL_CODE)).thenReturn(JWT_TOKEN);

        // Act
        Optional<LoginResponse> result = authService.authenticate(request);

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().getAccessToken()).isEqualTo(JWT_TOKEN);
        assertThat(result.get().getHotelName()).isEqualTo(HOTEL_NAME);
        verify(hotelRepository).findByHotelCode(HOTEL_CODE);
        verify(passwordEncoder).matches(HOTEL_PASSWORD, hotel.getPasswordHash());
        verify(jwtService).generateToken(HOTEL_ID, HOTEL_CODE);
    }

    @Test
    void authenticate_WithInvalidHotelCode_ReturnsEmpty() {
        // Arrange
        LoginRequest request = new LoginRequest("INVALID_CODE", HOTEL_PASSWORD);
        when(hotelRepository.findByHotelCode("INVALID_CODE")).thenReturn(Optional.empty());

        // Act
        Optional<LoginResponse> result = authService.authenticate(request);

        // Assert
        assertThat(result).isEmpty();
        verify(hotelRepository).findByHotelCode("INVALID_CODE");
        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(jwtService, never()).generateToken(any(), anyString());
    }

    @Test
    void authenticate_WithInvalidHotelPassword_ReturnsEmpty() {
        // Arrange
        LoginRequest request = new LoginRequest(HOTEL_CODE, "wrongPassword");
        Hotel hotel = new Hotel();
        hotel.setHotelId(HOTEL_ID);
        hotel.setHotelCode(HOTEL_CODE);
        hotel.setHotelName(HOTEL_NAME);
        hotel.setPasswordHash("hashedPassword");

        when(hotelRepository.findByHotelCode(HOTEL_CODE)).thenReturn(Optional.of(hotel));
        when(passwordEncoder.matches("wrongPassword", hotel.getPasswordHash())).thenReturn(false);

        // Act
        Optional<LoginResponse> result = authService.authenticate(request);

        // Assert
        assertThat(result).isEmpty();
        verify(hotelRepository).findByHotelCode(HOTEL_CODE);
        verify(passwordEncoder).matches("wrongPassword", hotel.getPasswordHash());
        verify(jwtService, never()).generateToken(any(), anyString());
    }

    @Test
    void authenticate_WithValidAdminCredentials_ReturnsLoginResponse() {
        // Arrange
        LoginRequest request = new LoginRequest(ADMIN_CODE, "admin123");
        Hotel admin = new Hotel();
        admin.setHotelId(99L);
        admin.setHotelCode(ADMIN_CODE);
        admin.setHotelName("System Administrator");
        admin.setPasswordHash("hashedAdminPassword");
        admin.setAdmin(true);

        when(hotelRepository.findByHotelCode(ADMIN_CODE)).thenReturn(Optional.of(admin));
        when(passwordEncoder.matches("admin123", admin.getPasswordHash())).thenReturn(true);
        when(jwtService.generateAdminToken(ADMIN_CODE)).thenReturn(JWT_TOKEN);

        // Act
        Optional<LoginResponse> result = authService.authenticate(request);

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().getAccessToken()).isEqualTo(JWT_TOKEN);
        assertThat(result.get().getHotelName()).isEqualTo("System Administrator");
        verify(jwtService).generateAdminToken(ADMIN_CODE);
    }

    @Test
    void authenticate_WithInvalidAdminPassword_ReturnsEmpty() {
        // Arrange
        LoginRequest request = new LoginRequest(ADMIN_CODE, "wrongPassword");
        Hotel admin = new Hotel();
        admin.setHotelId(99L);
        admin.setHotelCode(ADMIN_CODE);
        admin.setHotelName("System Administrator");
        admin.setPasswordHash("hashedAdminPassword");
        admin.setAdmin(true);

        when(hotelRepository.findByHotelCode(ADMIN_CODE)).thenReturn(Optional.of(admin));
        when(passwordEncoder.matches("wrongPassword", admin.getPasswordHash())).thenReturn(false);

        // Act
        Optional<LoginResponse> result = authService.authenticate(request);

        // Assert
        assertThat(result).isEmpty();
        verify(passwordEncoder).matches("wrongPassword", admin.getPasswordHash());
        verify(jwtService, never()).generateAdminToken(anyString());
    }

    @Test
    void authenticate_WithNonExistentAdmin_ReturnsEmpty() {
        // Arrange
        LoginRequest request = new LoginRequest(ADMIN_CODE, "admin123");
        when(hotelRepository.findByHotelCode(ADMIN_CODE)).thenReturn(Optional.empty());

        // Act
        Optional<LoginResponse> result = authService.authenticate(request);

        // Assert
        assertThat(result).isEmpty();
        verify(hotelRepository).findByHotelCode(ADMIN_CODE);
        verify(jwtService, never()).generateAdminToken(anyString());
    }
}
