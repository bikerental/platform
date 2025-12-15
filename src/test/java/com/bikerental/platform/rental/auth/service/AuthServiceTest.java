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
import org.mockito.InjectMocks;
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

    @InjectMocks
    private AuthService authService;

    private static final String ADMIN_USERNAME = "admin";
    private static final String ADMIN_PASSWORD_HASH = "hashed-admin-password";
    private static final String HOTEL_CODE = "HOTEL001";
    private static final String HOTEL_PASSWORD = "hotelPassword123";
    private static final String HOTEL_NAME = "Test Hotel";
    private static final Long HOTEL_ID = 1L;
    private static final String JWT_TOKEN = "test.jwt.token";

    @BeforeEach
    void setUp() {
        // Use reflection or constructor injection - for now, we'll test with actual constructor
        // Since AuthService uses constructor injection with @Value, we need to create it manually
    }

    @Test
    void authenticate_WithValidHotelCredentials_ReturnsLoginResponse() {
        // Arrange
        AuthService service = new AuthService(
                hotelRepository,
                passwordEncoder,
                jwtService,
                ADMIN_USERNAME,
                ADMIN_PASSWORD_HASH 
        );

        LoginRequest request = new LoginRequest(HOTEL_CODE, HOTEL_PASSWORD);
        Hotel hotel = new Hotel();
        hotel.setHotelId(HOTEL_ID);
        hotel.setHotelCode(HOTEL_CODE);
        hotel.setHotelName(HOTEL_NAME);
        hotel.setPasswordHash("hashedPassword");

        when(hotelRepository.findByHotelCode(HOTEL_CODE)).thenReturn(Optional.of(hotel));
        when(passwordEncoder.matches(HOTEL_PASSWORD, hotel.getPasswordHash())).thenReturn(true);
        when(jwtService.generateToken(HOTEL_ID, HOTEL_CODE)).thenReturn(JWT_TOKEN);

        // Act
        Optional<LoginResponse> result = service.authenticate(request);

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
        AuthService service = new AuthService(
                hotelRepository,
                passwordEncoder,
                jwtService,
                ADMIN_USERNAME,
                ADMIN_PASSWORD_HASH 
        );

        LoginRequest request = new LoginRequest("INVALID_CODE", HOTEL_PASSWORD);
        when(hotelRepository.findByHotelCode("INVALID_CODE")).thenReturn(Optional.empty());

        // Act
        Optional<LoginResponse> result = service.authenticate(request);

        // Assert
        assertThat(result).isEmpty();
        verify(hotelRepository).findByHotelCode("INVALID_CODE");
        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(jwtService, never()).generateToken(any(), anyString());
    }

    @Test
    void authenticate_WithInvalidHotelPassword_ReturnsEmpty() {
        // Arrange
        AuthService service = new AuthService(
                hotelRepository,
                passwordEncoder,
                jwtService,
                ADMIN_USERNAME,
                ADMIN_PASSWORD_HASH 
        );

        LoginRequest request = new LoginRequest(HOTEL_CODE, "wrongPassword");
        Hotel hotel = new Hotel();
        hotel.setHotelId(HOTEL_ID);
        hotel.setHotelCode(HOTEL_CODE);
        hotel.setHotelName(HOTEL_NAME);
        hotel.setPasswordHash("hashedPassword");

        when(hotelRepository.findByHotelCode(HOTEL_CODE)).thenReturn(Optional.of(hotel));
        when(passwordEncoder.matches("wrongPassword", hotel.getPasswordHash())).thenReturn(false);

        // Act
        Optional<LoginResponse> result = service.authenticate(request);

        // Assert
        assertThat(result).isEmpty();
        verify(hotelRepository).findByHotelCode(HOTEL_CODE);
        verify(passwordEncoder).matches("wrongPassword", hotel.getPasswordHash());
        verify(jwtService, never()).generateToken(any(), anyString());
    }

    @Test
    void authenticate_WithValidAdminCredentials_ReturnsLoginResponse() {
        AuthService service = new AuthService(
            hotelRepository,
            passwordEncoder,
            jwtService,
            ADMIN_USERNAME,
            ADMIN_PASSWORD_HASH
        );

        LoginRequest request = new LoginRequest(ADMIN_USERNAME, "admin123");

        when(passwordEncoder.matches("admin123", ADMIN_PASSWORD_HASH)).thenReturn(true);
        when(jwtService.generateAdminToken(ADMIN_USERNAME)).thenReturn(JWT_TOKEN);

        Optional<LoginResponse> result = service.authenticate(request);

        assertThat(result).isPresent();
        assertThat(result.get().getAccessToken()).isEqualTo(JWT_TOKEN);
        assertThat(result.get().getHotelName()).isEqualTo("System Administrator");
        verify(passwordEncoder).matches("admin123", ADMIN_PASSWORD_HASH);
        verify(jwtService).generateAdminToken(ADMIN_USERNAME);
    }

    @Test
    void authenticate_WithInvalidAdminPassword_ReturnsEmpty() {
        // Arrange
        AuthService service = new AuthService(
                hotelRepository,
                passwordEncoder,
                jwtService,
                ADMIN_USERNAME,
                ADMIN_PASSWORD_HASH 
        );

        LoginRequest request = new LoginRequest(ADMIN_USERNAME, "wrongPassword");
        when(passwordEncoder.matches("wrongPassword", ADMIN_PASSWORD_HASH)).thenReturn(false);

        // Act
        Optional<LoginResponse> result = service.authenticate(request);

        // Assert
        assertThat(result).isEmpty();

        verify(passwordEncoder).matches("wrongPassword", ADMIN_PASSWORD_HASH);
        verify(jwtService, never()).generateAdminToken(anyString());
        verify(hotelRepository, never()).findByHotelCode(anyString());
    }

    @Test
    void authenticate_WithAdminUsernameButWrongPassword_ReturnsEmpty() {
        // Arrange
        AuthService service = new AuthService(
                hotelRepository,
                passwordEncoder,
                jwtService,
                ADMIN_USERNAME,
                ADMIN_PASSWORD_HASH 
        );

        LoginRequest request = new LoginRequest(ADMIN_USERNAME, "wrongPassword");

        // Act
        when(passwordEncoder.matches("wrongPassword", ADMIN_PASSWORD_HASH)).thenReturn(false);
        Optional<LoginResponse> result = service.authenticate(request);

        // Assert
        assertThat(result).isEmpty();
        verify(passwordEncoder).matches("wrongPassword", ADMIN_PASSWORD_HASH);
        verify(jwtService, never()).generateAdminToken(anyString());
    }
}
