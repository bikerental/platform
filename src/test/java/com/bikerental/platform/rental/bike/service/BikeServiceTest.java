package com.bikerental.platform.rental.bike.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.bikerental.platform.rental.auth.security.HotelContext;
import com.bikerental.platform.rental.bike.model.Bike;
import com.bikerental.platform.rental.bike.repo.BikeRepository;
import com.bikerental.platform.rental.common.exception.ConflictException;
import com.bikerental.platform.rental.common.exception.NotFoundException;

@ExtendWith(MockitoExtension.class)
class BikeServiceTest {

    @Mock
    private BikeRepository bikeRepository;

    @Mock
    private HotelContext hotelContext;

    @InjectMocks
    private BikeService bikeService;

    private static final Long HOTEL_ID_1 = 1L;
    private static final Long HOTEL_ID_2 = 2L;
    private static final Long BIKE_ID_1 = 10L;
    private static final String BIKE_NUMBER_1 = "B001";

    private Bike availableBike;
    private Bike rentedBike;
    private Bike oooBike;

    @BeforeEach
    void setUp() {
        availableBike = new Bike();
        availableBike.setBikeId(BIKE_ID_1);
        availableBike.setHotelId(HOTEL_ID_1);
        availableBike.setBikeNumber(BIKE_NUMBER_1);
        availableBike.setBikeType("ADULT");
        availableBike.setStatus(Bike.BikeStatus.AVAILABLE);
        availableBike.setOooNote(null);
        availableBike.setOooSince(null);

        rentedBike = new Bike();
        rentedBike.setBikeId(BIKE_ID_1);
        rentedBike.setHotelId(HOTEL_ID_1);
        rentedBike.setBikeNumber(BIKE_NUMBER_1);
        rentedBike.setBikeType("ADULT");
        rentedBike.setStatus(Bike.BikeStatus.RENTED);
        rentedBike.setOooNote(null);
        rentedBike.setOooSince(null);

        oooBike = new Bike();
        oooBike.setBikeId(BIKE_ID_1);
        oooBike.setHotelId(HOTEL_ID_1);
        oooBike.setBikeNumber(BIKE_NUMBER_1);
        oooBike.setBikeType("ADULT");
        oooBike.setStatus(Bike.BikeStatus.OOO);
        oooBike.setOooNote("Flat tire");
        oooBike.setOooSince(Instant.now().minusSeconds(3600));
    }

    @Test
    void listBikes_ScopedByHotelId_ReturnsOnlyBikesForCurrentHotel() {
        // Arrange
        when(hotelContext.getCurrentHotelId()).thenReturn(HOTEL_ID_1);
        
        Bike bike1 = new Bike();
        bike1.setBikeId(1L);
        bike1.setHotelId(HOTEL_ID_1);
        bike1.setBikeNumber("B001");
        bike1.setStatus(Bike.BikeStatus.AVAILABLE);
        
        Bike bike2 = new Bike();
        bike2.setBikeId(2L);
        bike2.setHotelId(HOTEL_ID_1);
        bike2.setBikeNumber("B002");
        bike2.setStatus(Bike.BikeStatus.AVAILABLE);
        
        List<Bike> expectedBikes = Arrays.asList(bike1, bike2);
        when(bikeRepository.findByHotelIdWithFilters(HOTEL_ID_1, null, null))
                .thenReturn(expectedBikes);

        // Act
        List<Bike> result = bikeService.listBikes(null, null);

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result).containsExactlyElementsOf(expectedBikes);
        verify(hotelContext).getCurrentHotelId();
        verify(bikeRepository).findByHotelIdWithFilters(HOTEL_ID_1, null, null);
    }

    @Test
    void listBikes_WithOooStatusFilter_UsesOooBikesQuery() {
        // Arrange
        when(hotelContext.getCurrentHotelId()).thenReturn(HOTEL_ID_1);
        
        Bike oooBike = new Bike();
        oooBike.setBikeId(3L);
        oooBike.setHotelId(HOTEL_ID_1);
        oooBike.setBikeNumber("B003");
        oooBike.setStatus(Bike.BikeStatus.OOO);
        
        List<Bike> expectedBikes = Arrays.asList(oooBike);
        // OOO status now uses the specialized query with proper sorting
        when(bikeRepository.findOooBikesWithFilters(HOTEL_ID_1, null))
                .thenReturn(expectedBikes);

        // Act
        List<Bike> result = bikeService.listBikes(Bike.BikeStatus.OOO, null);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo(Bike.BikeStatus.OOO);
        verify(bikeRepository).findOooBikesWithFilters(HOTEL_ID_1, null);
    }

    @Test
    void listBikes_WithNonOooStatusFilter_UsesGenericQuery() {
        // Arrange
        when(hotelContext.getCurrentHotelId()).thenReturn(HOTEL_ID_1);
        
        Bike availableBike = new Bike();
        availableBike.setBikeId(3L);
        availableBike.setHotelId(HOTEL_ID_1);
        availableBike.setBikeNumber("B003");
        availableBike.setStatus(Bike.BikeStatus.AVAILABLE);
        
        List<Bike> expectedBikes = Arrays.asList(availableBike);
        when(bikeRepository.findByHotelIdWithFilters(HOTEL_ID_1, "AVAILABLE", null))
                .thenReturn(expectedBikes);

        // Act
        List<Bike> result = bikeService.listBikes(Bike.BikeStatus.AVAILABLE, null);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo(Bike.BikeStatus.AVAILABLE);
        verify(bikeRepository).findByHotelIdWithFilters(HOTEL_ID_1, "AVAILABLE", null);
    }

    @Test
    void markOoo_SetsStatusNoteAndTimestamp() {
        // Arrange
        when(hotelContext.getCurrentHotelId()).thenReturn(HOTEL_ID_1);
        when(bikeRepository.findById(BIKE_ID_1)).thenReturn(Optional.of(availableBike));
        when(bikeRepository.save(any(Bike.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        String note = "Needs repair";

        // Act
        Bike result = bikeService.markOoo(BIKE_ID_1, note);

        // Assert
        assertThat(result.getStatus()).isEqualTo(Bike.BikeStatus.OOO);
        assertThat(result.getOooNote()).isEqualTo(note);
        assertThat(result.getOooSince()).isNotNull();
        assertThat(result.getOooSince()).isBeforeOrEqualTo(Instant.now());
        verify(bikeRepository).save(availableBike);
    }

    @Test
    void markAvailable_ClearsOooFields() {
        // Arrange
        when(hotelContext.getCurrentHotelId()).thenReturn(HOTEL_ID_1);
        when(bikeRepository.findById(BIKE_ID_1)).thenReturn(Optional.of(oooBike));
        when(bikeRepository.save(any(Bike.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Bike result = bikeService.markAvailable(BIKE_ID_1);

        // Assert
        assertThat(result.getStatus()).isEqualTo(Bike.BikeStatus.AVAILABLE);
        assertThat(result.getOooNote()).isNull();
        assertThat(result.getOooSince()).isNull();
        verify(bikeRepository).save(oooBike);
    }

    @Test
    void markAvailable_WhenStatusIsRented_ThrowsConflictException() {
        // Arrange
        when(hotelContext.getCurrentHotelId()).thenReturn(HOTEL_ID_1);
        when(bikeRepository.findById(BIKE_ID_1)).thenReturn(Optional.of(rentedBike));

        // Act & Assert
        assertThatThrownBy(() -> bikeService.markAvailable(BIKE_ID_1))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("currently rented");
        
        verify(bikeRepository, never()).save(any(Bike.class));
    }

    @Test
    void markAvailable_WhenBikeNotFound_ThrowsNotFoundException() {
        // Arrange
        when(hotelContext.getCurrentHotelId()).thenReturn(HOTEL_ID_1);
        when(bikeRepository.findById(BIKE_ID_1)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> bikeService.markAvailable(BIKE_ID_1))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Bike not found");
        
        verify(bikeRepository, never()).save(any(Bike.class));
    }

    @Test
    void markAvailable_WhenBikeBelongsToDifferentHotel_ThrowsNotFoundException() {
        // Arrange
        Bike otherHotelBike = new Bike();
        otherHotelBike.setBikeId(BIKE_ID_1);
        otherHotelBike.setHotelId(HOTEL_ID_2);
        otherHotelBike.setStatus(Bike.BikeStatus.AVAILABLE);
        
        when(hotelContext.getCurrentHotelId()).thenReturn(HOTEL_ID_1);
        when(bikeRepository.findById(BIKE_ID_1)).thenReturn(Optional.of(otherHotelBike));

        // Act & Assert
        assertThatThrownBy(() -> bikeService.markAvailable(BIKE_ID_1))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Bike not found");
        
        verify(bikeRepository, never()).save(any(Bike.class));
    }

    @Test
    void findByBikeNumber_ScopedByHotelId_ReturnsBike() {
        // Arrange
        when(hotelContext.getCurrentHotelId()).thenReturn(HOTEL_ID_1);
        when(bikeRepository.findByHotelIdAndBikeNumber(HOTEL_ID_1, BIKE_NUMBER_1))
                .thenReturn(Optional.of(availableBike));

        // Act
        Bike result = bikeService.findByBikeNumber(BIKE_NUMBER_1);

        // Assert
        assertThat(result).isEqualTo(availableBike);
        verify(hotelContext).getCurrentHotelId();
        verify(bikeRepository).findByHotelIdAndBikeNumber(HOTEL_ID_1, BIKE_NUMBER_1);
    }

    @Test
    void findByBikeNumber_WhenNotFound_ThrowsNotFoundException() {
        // Arrange
        when(hotelContext.getCurrentHotelId()).thenReturn(HOTEL_ID_1);
        when(bikeRepository.findByHotelIdAndBikeNumber(HOTEL_ID_1, BIKE_NUMBER_1))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> bikeService.findByBikeNumber(BIKE_NUMBER_1))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Bike not found");
    }
}

