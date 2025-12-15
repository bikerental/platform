package com.bikerental.platform.rental.admin.service;

import com.bikerental.platform.rental.admin.dto.CreateHotelRequest;
import com.bikerental.platform.rental.admin.dto.HotelResponse;
import com.bikerental.platform.rental.auth.model.Hotel;
import com.bikerental.platform.rental.auth.repo.HotelRepository;
import com.bikerental.platform.rental.common.exception.ConflictException;
import com.bikerental.platform.rental.common.exception.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock
    private HotelRepository hotelRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AdminService adminService;

    private static final String HOTEL_CODE = "HOTEL001";
    private static final String HOTEL_NAME = "Test Hotel";
    private static final String PASSWORD = "password123";
    private static final String HASHED_PASSWORD = "$2a$10$hashedPasswordHash";
    private static final Long HOTEL_ID = 1L;

    @BeforeEach
    void setUp() {
        // Service is already injected via @InjectMocks
    }

    @Test
    void createHotel_WithValidRequest_CreatesHotel() {
        // Arrange
        CreateHotelRequest request = new CreateHotelRequest(HOTEL_CODE, HOTEL_NAME, PASSWORD);
        when(hotelRepository.existsByHotelCode(HOTEL_CODE)).thenReturn(false);
        when(passwordEncoder.encode(PASSWORD)).thenReturn(HASHED_PASSWORD);

        Hotel savedHotel = new Hotel();
        savedHotel.setHotelId(HOTEL_ID);
        savedHotel.setHotelCode(HOTEL_CODE);
        savedHotel.setHotelName(HOTEL_NAME);
        savedHotel.setPasswordHash(HASHED_PASSWORD);
        savedHotel.setCreatedAt(Instant.now());

        when(hotelRepository.save(any(Hotel.class))).thenReturn(savedHotel);

        // Act
        HotelResponse response = adminService.createHotel(request);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getHotelId()).isEqualTo(HOTEL_ID);
        assertThat(response.getHotelCode()).isEqualTo(HOTEL_CODE);
        assertThat(response.getHotelName()).isEqualTo(HOTEL_NAME);
        assertThat(response.getCreatedAt()).isNotNull();

        ArgumentCaptor<Hotel> hotelCaptor = ArgumentCaptor.forClass(Hotel.class);
        verify(hotelRepository).save(hotelCaptor.capture());
        Hotel capturedHotel = hotelCaptor.getValue();
        assertThat(capturedHotel.getHotelCode()).isEqualTo(HOTEL_CODE);
        assertThat(capturedHotel.getHotelName()).isEqualTo(HOTEL_NAME);
        assertThat(capturedHotel.getPasswordHash()).isEqualTo(HASHED_PASSWORD);

        verify(hotelRepository).existsByHotelCode(HOTEL_CODE);
        verify(passwordEncoder).encode(PASSWORD);
    }

    @Test
    void createHotel_WithDuplicateHotelCode_ThrowsConflictException() {
        // Arrange
        CreateHotelRequest request = new CreateHotelRequest(HOTEL_CODE, HOTEL_NAME, PASSWORD);
        when(hotelRepository.existsByHotelCode(HOTEL_CODE)).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> adminService.createHotel(request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Hotel code '" + HOTEL_CODE + "' already exists");

        verify(hotelRepository).existsByHotelCode(HOTEL_CODE);
        verify(hotelRepository, never()).save(any(Hotel.class));
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    void resetPassword_WithValidHotelId_UpdatesPassword() {
        // Arrange
        String newPassword = "newPassword123";
        String newHashedPassword = "$2a$10$newHashedPassword";

        Hotel hotel = new Hotel();
        hotel.setHotelId(HOTEL_ID);
        hotel.setHotelCode(HOTEL_CODE);
        hotel.setHotelName(HOTEL_NAME);
        hotel.setPasswordHash(HASHED_PASSWORD);

        when(hotelRepository.findById(HOTEL_ID)).thenReturn(Optional.of(hotel));
        when(passwordEncoder.encode(newPassword)).thenReturn(newHashedPassword);
        when(hotelRepository.save(any(Hotel.class))).thenReturn(hotel);

        // Act
        adminService.resetPassword(HOTEL_ID, newPassword);

        // Assert
        ArgumentCaptor<Hotel> hotelCaptor = ArgumentCaptor.forClass(Hotel.class);
        verify(hotelRepository).save(hotelCaptor.capture());
        Hotel savedHotel = hotelCaptor.getValue();
        assertThat(savedHotel.getPasswordHash()).isEqualTo(newHashedPassword);

        verify(hotelRepository).findById(HOTEL_ID);
        verify(passwordEncoder).encode(newPassword);
    }

    @Test
    void resetPassword_WithInvalidHotelId_ThrowsNotFoundException() {
        // Arrange
        Long invalidHotelId = 999L;
        String newPassword = "newPassword123";

        when(hotelRepository.findById(invalidHotelId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> adminService.resetPassword(invalidHotelId, newPassword))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Hotel not found with ID: " + invalidHotelId);

        verify(hotelRepository).findById(invalidHotelId);
        verify(passwordEncoder, never()).encode(anyString());
        verify(hotelRepository, never()).save(any(Hotel.class));
    }

    @Test
    void getAllHotels_ReturnsListOfHotels() {
        // Arrange
        Hotel hotel1 = new Hotel();
        hotel1.setHotelId(1L);
        hotel1.setHotelCode("HOTEL001");
        hotel1.setHotelName("Hotel One");
        hotel1.setCreatedAt(Instant.now());

        Hotel hotel2 = new Hotel();
        hotel2.setHotelId(2L);
        hotel2.setHotelCode("HOTEL002");
        hotel2.setHotelName("Hotel Two");
        hotel2.setCreatedAt(Instant.now());

        when(hotelRepository.findAll()).thenReturn(List.of(hotel1, hotel2));

        // Act
        List<HotelResponse> responses = adminService.getAllHotels();

        // Assert
        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).getHotelId()).isEqualTo(1L);
        assertThat(responses.get(0).getHotelCode()).isEqualTo("HOTEL001");
        assertThat(responses.get(0).getHotelName()).isEqualTo("Hotel One");
        assertThat(responses.get(1).getHotelId()).isEqualTo(2L);
        assertThat(responses.get(1).getHotelCode()).isEqualTo("HOTEL002");
        assertThat(responses.get(1).getHotelName()).isEqualTo("Hotel Two");

        verify(hotelRepository).findAll();
    }

    @Test
    void getAllHotels_WithNoHotels_ReturnsEmptyList() {
        // Arrange
        when(hotelRepository.findAll()).thenReturn(List.of());

        // Act
        List<HotelResponse> responses = adminService.getAllHotels();

        // Assert
        assertThat(responses).isEmpty();
        verify(hotelRepository).findAll();
    }
}
