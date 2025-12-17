package com.bikerental.platform.rental.rentals.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.bikerental.platform.rental.auth.security.HotelContext;
import com.bikerental.platform.rental.bike.model.Bike;
import com.bikerental.platform.rental.bike.repo.BikeRepository;
import com.bikerental.platform.rental.common.exception.BikeUnavailableException;
import com.bikerental.platform.rental.rentals.dto.CreateRentalRequest;
import com.bikerental.platform.rental.rentals.dto.RentalResponse;
import com.bikerental.platform.rental.rentals.model.Rental;
import com.bikerental.platform.rental.rentals.model.RentalItem;
import com.bikerental.platform.rental.rentals.model.RentalItemStatus;
import com.bikerental.platform.rental.rentals.model.RentalStatus;
import com.bikerental.platform.rental.rentals.repo.RentalRepository;
import com.bikerental.platform.rental.signature.service.SignatureService;

@ExtendWith(MockitoExtension.class)
class RentalServiceTest {

    @Mock
    private RentalRepository rentalRepository;

    @Mock
    private BikeRepository bikeRepository;

    @Mock
    private SignatureService signatureService;

    @Mock
    private HotelContext hotelContext;

    @InjectMocks
    private RentalService rentalService;

    private static final Long HOTEL_ID = 1L;
    private static final Long SIGNATURE_ID = 100L;
    private static final String ROOM_NUMBER = "204";
    private static final String BED_NUMBER = "A";
    private static final String TNC_VERSION = "v1";
    private static final byte[] PNG_BYTES = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47};
    private static final String SIGNATURE_BASE64 = Base64.getEncoder().encodeToString(PNG_BYTES);

    private Bike bike1;
    private Bike bike2;
    private Instant futureReturnTime;

    @BeforeEach
    void setUp() {
        bike1 = new Bike();
        bike1.setBikeId(10L);
        bike1.setHotelId(HOTEL_ID);
        bike1.setBikeNumber("B001");
        bike1.setBikeType("ADULT");
        bike1.setStatus(Bike.BikeStatus.AVAILABLE);

        bike2 = new Bike();
        bike2.setBikeId(20L);
        bike2.setHotelId(HOTEL_ID);
        bike2.setBikeNumber("B002");
        bike2.setBikeType("CHILD");
        bike2.setStatus(Bike.BikeStatus.AVAILABLE);

        futureReturnTime = Instant.now().plus(24, ChronoUnit.HOURS);
    }

    // ====== Happy Path Tests ======

    @Test
    void createRental_WithSingleBike_CreatesRentalSuccessfully() {
        // Arrange
        when(hotelContext.getCurrentHotelId()).thenReturn(HOTEL_ID);
        when(bikeRepository.findByHotelIdAndBikeNumber(HOTEL_ID, "B001")).thenReturn(Optional.of(bike1));
        when(signatureService.storeSignature(eq(HOTEL_ID), eq(SIGNATURE_BASE64))).thenReturn(SIGNATURE_ID);
        when(rentalRepository.save(any(Rental.class))).thenAnswer(invocation -> {
            Rental r = invocation.getArgument(0);
            r.setRentalId(1L);
            // Simulate JPA setting IDs on items
            for (int i = 0; i < r.getItems().size(); i++) {
                r.getItems().get(i).setRentalItemId((long) (i + 1));
            }
            return r;
        });

        CreateRentalRequest request = new CreateRentalRequest(
                List.of("B001"), ROOM_NUMBER, BED_NUMBER, futureReturnTime, TNC_VERSION, SIGNATURE_BASE64
        );

        // Act
        RentalResponse response = rentalService.createRental(request);

        // Assert
        assertThat(response.getRentalId()).isEqualTo(1L);
        assertThat(response.getStatus()).isEqualTo(RentalStatus.ACTIVE);
        assertThat(response.getRoomNumber()).isEqualTo(ROOM_NUMBER);
        assertThat(response.getBedNumber()).isEqualTo(BED_NUMBER);
        assertThat(response.getDueAt()).isEqualTo(futureReturnTime);
        assertThat(response.getStartAt()).isNotNull();
        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems().get(0).getBikeNumber()).isEqualTo("B001");
        assertThat(response.getItems().get(0).getStatus()).isEqualTo(RentalItemStatus.RENTED);

        // Verify bike status was updated to RENTED
        verify(bikeRepository).save(bike1);
        assertThat(bike1.getStatus()).isEqualTo(Bike.BikeStatus.RENTED);
    }

    @Test
    void createRental_WithMultipleBikes_CreatesRentalWithAllItems() {
        // Arrange
        when(hotelContext.getCurrentHotelId()).thenReturn(HOTEL_ID);
        when(bikeRepository.findByHotelIdAndBikeNumber(HOTEL_ID, "B001")).thenReturn(Optional.of(bike1));
        when(bikeRepository.findByHotelIdAndBikeNumber(HOTEL_ID, "B002")).thenReturn(Optional.of(bike2));
        when(signatureService.storeSignature(eq(HOTEL_ID), eq(SIGNATURE_BASE64))).thenReturn(SIGNATURE_ID);
        when(rentalRepository.save(any(Rental.class))).thenAnswer(invocation -> {
            Rental r = invocation.getArgument(0);
            r.setRentalId(1L);
            for (int i = 0; i < r.getItems().size(); i++) {
                r.getItems().get(i).setRentalItemId((long) (i + 1));
            }
            return r;
        });

        CreateRentalRequest request = new CreateRentalRequest(
                Arrays.asList("B001", "B002"), ROOM_NUMBER, BED_NUMBER, futureReturnTime, TNC_VERSION, SIGNATURE_BASE64
        );

        // Act
        RentalResponse response = rentalService.createRental(request);

        // Assert
        assertThat(response.getItems()).hasSize(2);
        assertThat(response.getItems()).extracting("bikeNumber").containsExactlyInAnyOrder("B001", "B002");

        // Verify both bikes were updated to RENTED
        verify(bikeRepository, times(2)).save(any(Bike.class));
        assertThat(bike1.getStatus()).isEqualTo(Bike.BikeStatus.RENTED);
        assertThat(bike2.getStatus()).isEqualTo(Bike.BikeStatus.RENTED);
    }

    @Test
    void createRental_WithNullBedNumber_CreatesRentalSuccessfully() {
        // Arrange
        when(hotelContext.getCurrentHotelId()).thenReturn(HOTEL_ID);
        when(bikeRepository.findByHotelIdAndBikeNumber(HOTEL_ID, "B001")).thenReturn(Optional.of(bike1));
        when(signatureService.storeSignature(eq(HOTEL_ID), eq(SIGNATURE_BASE64))).thenReturn(SIGNATURE_ID);
        when(rentalRepository.save(any(Rental.class))).thenAnswer(invocation -> {
            Rental r = invocation.getArgument(0);
            r.setRentalId(1L);
            r.getItems().get(0).setRentalItemId(1L);
            return r;
        });

        CreateRentalRequest request = new CreateRentalRequest(
                List.of("B001"), ROOM_NUMBER, null, futureReturnTime, TNC_VERSION, SIGNATURE_BASE64
        );

        // Act
        RentalResponse response = rentalService.createRental(request);

        // Assert
        assertThat(response.getBedNumber()).isNull();
    }

    // ====== Validation Tests ======

    @Test
    void createRental_WithEmptyBikeList_ThrowsIllegalArgumentException() {
        // Arrange
        when(hotelContext.getCurrentHotelId()).thenReturn(HOTEL_ID);
        CreateRentalRequest request = new CreateRentalRequest(
                List.of(), ROOM_NUMBER, BED_NUMBER, futureReturnTime, TNC_VERSION, SIGNATURE_BASE64
        );

        // Act & Assert
        assertThatThrownBy(() -> rentalService.createRental(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("At least one bike is required");

        verify(rentalRepository, never()).save(any());
    }

    @Test
    void createRental_WithNullBikeList_ThrowsIllegalArgumentException() {
        // Arrange
        when(hotelContext.getCurrentHotelId()).thenReturn(HOTEL_ID);
        CreateRentalRequest request = new CreateRentalRequest(
                null, ROOM_NUMBER, BED_NUMBER, futureReturnTime, TNC_VERSION, SIGNATURE_BASE64
        );

        // Act & Assert
        assertThatThrownBy(() -> rentalService.createRental(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("At least one bike is required");

        verify(rentalRepository, never()).save(any());
    }

    @Test
    void createRental_WithPastReturnTime_ThrowsIllegalArgumentException() {
        // Arrange
        when(hotelContext.getCurrentHotelId()).thenReturn(HOTEL_ID);
        Instant pastTime = Instant.now().minus(1, ChronoUnit.HOURS);
        CreateRentalRequest request = new CreateRentalRequest(
                List.of("B001"), ROOM_NUMBER, BED_NUMBER, pastTime, TNC_VERSION, SIGNATURE_BASE64
        );

        // Act & Assert
        assertThatThrownBy(() -> rentalService.createRental(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Return date/time must be in the future");

        verify(rentalRepository, never()).save(any());
    }

    // ====== Bike Availability Tests ======

    @Test
    void createRental_WithNonExistentBike_ThrowsBikeUnavailableException() {
        // Arrange
        when(hotelContext.getCurrentHotelId()).thenReturn(HOTEL_ID);
        when(bikeRepository.findByHotelIdAndBikeNumber(HOTEL_ID, "NOTFOUND")).thenReturn(Optional.empty());

        CreateRentalRequest request = new CreateRentalRequest(
                List.of("NOTFOUND"), ROOM_NUMBER, BED_NUMBER, futureReturnTime, TNC_VERSION, SIGNATURE_BASE64
        );

        // Act & Assert
        assertThatThrownBy(() -> rentalService.createRental(request))
                .isInstanceOf(BikeUnavailableException.class)
                .satisfies(ex -> {
                    BikeUnavailableException bue = (BikeUnavailableException) ex;
                    assertThat(bue.getUnavailableBikes()).hasSize(1);
                    assertThat(bue.getUnavailableBikes().get(0).getBikeNumber()).isEqualTo("NOTFOUND");
                    assertThat(bue.getUnavailableBikes().get(0).getReason()).isEqualTo("NOT_FOUND");
                });

        verify(rentalRepository, never()).save(any());
    }

    @Test
    void createRental_WithRentedBike_ThrowsBikeUnavailableException() {
        // Arrange
        when(hotelContext.getCurrentHotelId()).thenReturn(HOTEL_ID);
        bike1.setStatus(Bike.BikeStatus.RENTED);
        when(bikeRepository.findByHotelIdAndBikeNumber(HOTEL_ID, "B001")).thenReturn(Optional.of(bike1));

        CreateRentalRequest request = new CreateRentalRequest(
                List.of("B001"), ROOM_NUMBER, BED_NUMBER, futureReturnTime, TNC_VERSION, SIGNATURE_BASE64
        );

        // Act & Assert
        assertThatThrownBy(() -> rentalService.createRental(request))
                .isInstanceOf(BikeUnavailableException.class)
                .satisfies(ex -> {
                    BikeUnavailableException bue = (BikeUnavailableException) ex;
                    assertThat(bue.getUnavailableBikes()).hasSize(1);
                    assertThat(bue.getUnavailableBikes().get(0).getBikeNumber()).isEqualTo("B001");
                    assertThat(bue.getUnavailableBikes().get(0).getReason()).isEqualTo("ALREADY_RENTED");
                });

        verify(rentalRepository, never()).save(any());
    }

    @Test
    void createRental_WithOooBike_ThrowsBikeUnavailableException() {
        // Arrange
        when(hotelContext.getCurrentHotelId()).thenReturn(HOTEL_ID);
        bike1.setStatus(Bike.BikeStatus.OOO);
        when(bikeRepository.findByHotelIdAndBikeNumber(HOTEL_ID, "B001")).thenReturn(Optional.of(bike1));

        CreateRentalRequest request = new CreateRentalRequest(
                List.of("B001"), ROOM_NUMBER, BED_NUMBER, futureReturnTime, TNC_VERSION, SIGNATURE_BASE64
        );

        // Act & Assert
        assertThatThrownBy(() -> rentalService.createRental(request))
                .isInstanceOf(BikeUnavailableException.class)
                .satisfies(ex -> {
                    BikeUnavailableException bue = (BikeUnavailableException) ex;
                    assertThat(bue.getUnavailableBikes()).hasSize(1);
                    assertThat(bue.getUnavailableBikes().get(0).getBikeNumber()).isEqualTo("B001");
                    assertThat(bue.getUnavailableBikes().get(0).getReason()).isEqualTo("OUT_OF_ORDER");
                });

        verify(rentalRepository, never()).save(any());
    }

    @Test
    void createRental_WithMultipleUnavailableBikes_ReturnsAllInException() {
        // Arrange
        when(hotelContext.getCurrentHotelId()).thenReturn(HOTEL_ID);
        bike1.setStatus(Bike.BikeStatus.RENTED);
        bike2.setStatus(Bike.BikeStatus.OOO);
        when(bikeRepository.findByHotelIdAndBikeNumber(HOTEL_ID, "B001")).thenReturn(Optional.of(bike1));
        when(bikeRepository.findByHotelIdAndBikeNumber(HOTEL_ID, "B002")).thenReturn(Optional.of(bike2));
        when(bikeRepository.findByHotelIdAndBikeNumber(HOTEL_ID, "NOTFOUND")).thenReturn(Optional.empty());

        CreateRentalRequest request = new CreateRentalRequest(
                Arrays.asList("B001", "B002", "NOTFOUND"), ROOM_NUMBER, BED_NUMBER, futureReturnTime, TNC_VERSION, SIGNATURE_BASE64
        );

        // Act & Assert
        assertThatThrownBy(() -> rentalService.createRental(request))
                .isInstanceOf(BikeUnavailableException.class)
                .satisfies(ex -> {
                    BikeUnavailableException bue = (BikeUnavailableException) ex;
                    assertThat(bue.getUnavailableBikes()).hasSize(3);
                    assertThat(bue.getUnavailableBikes()).extracting("bikeNumber")
                            .containsExactlyInAnyOrder("B001", "B002", "NOTFOUND");
                });

        verify(rentalRepository, never()).save(any());
    }

    @Test
    void createRental_WithDuplicateBikeNumbers_ThrowsIllegalArgumentException() {
        // Arrange
        when(hotelContext.getCurrentHotelId()).thenReturn(HOTEL_ID);

        CreateRentalRequest request = new CreateRentalRequest(
                Arrays.asList("B001", "B001"), ROOM_NUMBER, BED_NUMBER, futureReturnTime, TNC_VERSION, SIGNATURE_BASE64
        );

        // Act & Assert
        assertThatThrownBy(() -> rentalService.createRental(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Duplicate bike number");

        verify(rentalRepository, never()).save(any());
    }

    // ====== Transaction/Atomicity Tests ======

    @Test
    void createRental_StoresSignatureBeforeRental() {
        // Arrange
        when(hotelContext.getCurrentHotelId()).thenReturn(HOTEL_ID);
        when(bikeRepository.findByHotelIdAndBikeNumber(HOTEL_ID, "B001")).thenReturn(Optional.of(bike1));
        when(signatureService.storeSignature(eq(HOTEL_ID), eq(SIGNATURE_BASE64))).thenReturn(SIGNATURE_ID);
        when(rentalRepository.save(any(Rental.class))).thenAnswer(invocation -> {
            Rental r = invocation.getArgument(0);
            r.setRentalId(1L);
            r.getItems().get(0).setRentalItemId(1L);
            return r;
        });

        CreateRentalRequest request = new CreateRentalRequest(
                List.of("B001"), ROOM_NUMBER, BED_NUMBER, futureReturnTime, TNC_VERSION, SIGNATURE_BASE64
        );

        // Act
        rentalService.createRental(request);

        // Assert - verify signature was stored and used
        verify(signatureService).storeSignature(HOTEL_ID, SIGNATURE_BASE64);
        
        ArgumentCaptor<Rental> rentalCaptor = ArgumentCaptor.forClass(Rental.class);
        verify(rentalRepository).save(rentalCaptor.capture());
        assertThat(rentalCaptor.getValue().getSignatureId()).isEqualTo(SIGNATURE_ID);
    }

    @Test
    void createRental_SetsCorrectRentalFields() {
        // Arrange
        when(hotelContext.getCurrentHotelId()).thenReturn(HOTEL_ID);
        when(bikeRepository.findByHotelIdAndBikeNumber(HOTEL_ID, "B001")).thenReturn(Optional.of(bike1));
        when(signatureService.storeSignature(eq(HOTEL_ID), eq(SIGNATURE_BASE64))).thenReturn(SIGNATURE_ID);
        when(rentalRepository.save(any(Rental.class))).thenAnswer(invocation -> {
            Rental r = invocation.getArgument(0);
            r.setRentalId(1L);
            r.getItems().get(0).setRentalItemId(1L);
            return r;
        });

        CreateRentalRequest request = new CreateRentalRequest(
                List.of("B001"), ROOM_NUMBER, BED_NUMBER, futureReturnTime, TNC_VERSION, SIGNATURE_BASE64
        );

        Instant beforeCreate = Instant.now();

        // Act
        rentalService.createRental(request);

        // Assert
        ArgumentCaptor<Rental> rentalCaptor = ArgumentCaptor.forClass(Rental.class);
        verify(rentalRepository).save(rentalCaptor.capture());
        
        Rental captured = rentalCaptor.getValue();
        assertThat(captured.getHotelId()).isEqualTo(HOTEL_ID);
        assertThat(captured.getStatus()).isEqualTo(RentalStatus.ACTIVE);
        assertThat(captured.getStartAt()).isBetween(beforeCreate, Instant.now().plusSeconds(1));
        assertThat(captured.getDueAt()).isEqualTo(futureReturnTime);
        assertThat(captured.getReturnAt()).isNull();
        assertThat(captured.getRoomNumber()).isEqualTo(ROOM_NUMBER);
        assertThat(captured.getBedNumber()).isEqualTo(BED_NUMBER);
        assertThat(captured.getTncVersion()).isEqualTo(TNC_VERSION);
        assertThat(captured.getSignatureId()).isEqualTo(SIGNATURE_ID);
    }

    @Test
    void createRental_SetsCorrectRentalItemFields() {
        // Arrange
        when(hotelContext.getCurrentHotelId()).thenReturn(HOTEL_ID);
        when(bikeRepository.findByHotelIdAndBikeNumber(HOTEL_ID, "B001")).thenReturn(Optional.of(bike1));
        when(signatureService.storeSignature(eq(HOTEL_ID), eq(SIGNATURE_BASE64))).thenReturn(SIGNATURE_ID);
        when(rentalRepository.save(any(Rental.class))).thenAnswer(invocation -> {
            Rental r = invocation.getArgument(0);
            r.setRentalId(1L);
            r.getItems().get(0).setRentalItemId(1L);
            return r;
        });

        CreateRentalRequest request = new CreateRentalRequest(
                List.of("B001"), ROOM_NUMBER, BED_NUMBER, futureReturnTime, TNC_VERSION, SIGNATURE_BASE64
        );

        // Act
        rentalService.createRental(request);

        // Assert
        ArgumentCaptor<Rental> rentalCaptor = ArgumentCaptor.forClass(Rental.class);
        verify(rentalRepository).save(rentalCaptor.capture());
        
        List<RentalItem> items = rentalCaptor.getValue().getItems();
        assertThat(items).hasSize(1);
        
        RentalItem item = items.get(0);
        assertThat(item.getBikeId()).isEqualTo(bike1.getBikeId());
        assertThat(item.getStatus()).isEqualTo(RentalItemStatus.RENTED);
        assertThat(item.getReturnedAt()).isNull();
        assertThat(item.getLostReason()).isNull();
    }

    // ====== Hotel Scoping Test ======

    @Test
    void createRental_UsesHotelIdFromContext() {
        // Arrange
        Long differentHotelId = 999L;
        when(hotelContext.getCurrentHotelId()).thenReturn(differentHotelId);
        
        // Bike lookup should use the hotel from context
        when(bikeRepository.findByHotelIdAndBikeNumber(differentHotelId, "B001")).thenReturn(Optional.empty());

        CreateRentalRequest request = new CreateRentalRequest(
                List.of("B001"), ROOM_NUMBER, BED_NUMBER, futureReturnTime, TNC_VERSION, SIGNATURE_BASE64
        );

        // Act & Assert
        assertThatThrownBy(() -> rentalService.createRental(request))
                .isInstanceOf(BikeUnavailableException.class);

        // Verify hotelId from context was used, not any other value
        verify(bikeRepository).findByHotelIdAndBikeNumber(differentHotelId, "B001");
        verify(bikeRepository, never()).findByHotelIdAndBikeNumber(eq(HOTEL_ID), any());
    }
}

