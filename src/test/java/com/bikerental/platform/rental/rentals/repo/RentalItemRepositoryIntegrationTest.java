package com.bikerental.platform.rental.rentals.repo;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import com.bikerental.platform.rental.rentals.model.Rental;
import com.bikerental.platform.rental.rentals.model.RentalItem;
import com.bikerental.platform.rental.rentals.model.RentalItemStatus;
import com.bikerental.platform.rental.rentals.model.RentalStatus;

/**
 * Integration tests for RentalItemRepository.
 * Includes tests for I1 invariant enforcement via repository queries.
 */
@DataJpaTest
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.show-sql=true",
    "spring.sql.init.mode=never"
})
@Transactional
class RentalItemRepositoryIntegrationTest {

    @Autowired
    private RentalItemRepository rentalItemRepository;

    @Autowired
    private RentalRepository rentalRepository;

    @Autowired
    private TestEntityManager entityManager;

    private static final Long HOTEL_ID = 1L;
    private static final Long SIGNATURE_ID = 100L;
    private static final Long BIKE_ID_1 = 10L;
    private static final Long BIKE_ID_2 = 20L;

    private Rental testRental;

    @BeforeEach
    void setUp() {
        rentalItemRepository.deleteAll();
        rentalRepository.deleteAll();
        
        // Create a test rental for use in item tests
        testRental = new Rental();
        testRental.setHotelId(HOTEL_ID);
        testRental.setStatus(RentalStatus.ACTIVE);
        testRental.setStartAt(Instant.now());
        testRental.setDueAt(Instant.now().plus(24, ChronoUnit.HOURS));
        testRental.setRoomNumber("101");
        testRental.setTncVersion("1.0");
        testRental.setSignatureId(SIGNATURE_ID);
        testRental = rentalRepository.save(testRental);
        entityManager.flush();
    }

    @Test
    void saveRentalItem_WithValidData_Succeeds() {
        // Arrange
        RentalItem item = new RentalItem(testRental, BIKE_ID_1);

        // Act
        RentalItem saved = rentalItemRepository.save(item);

        // Assert
        assertThat(saved.getRentalItemId()).isNotNull();
        assertThat(saved.getBikeId()).isEqualTo(BIKE_ID_1);
        assertThat(saved.getStatus()).isEqualTo(RentalItemStatus.RENTED);
    }

    @Test
    void findByRentalRentalId_ReturnsItemsForRental() {
        // Arrange
        RentalItem item1 = new RentalItem(testRental, BIKE_ID_1);
        RentalItem item2 = new RentalItem(testRental, BIKE_ID_2);
        rentalItemRepository.save(item1);
        rentalItemRepository.save(item2);

        // Act
        List<RentalItem> result = rentalItemRepository.findByRentalRentalId(testRental.getRentalId());

        // Assert
        assertThat(result).hasSize(2);
    }

    @Test
    void existsByBikeIdAndStatus_WhenRented_ReturnsTrue() {
        // Arrange
        RentalItem item = new RentalItem(testRental, BIKE_ID_1);
        item.setStatus(RentalItemStatus.RENTED);
        rentalItemRepository.save(item);

        // Act
        boolean exists = rentalItemRepository.existsByBikeIdAndStatus(BIKE_ID_1, RentalItemStatus.RENTED);

        // Assert
        assertThat(exists).isTrue();
    }

    @Test
    void existsByBikeIdAndStatus_WhenReturned_ReturnsFalseForRented() {
        // Arrange
        RentalItem item = new RentalItem(testRental, BIKE_ID_1);
        item.setStatus(RentalItemStatus.RETURNED);
        item.setReturnedAt(Instant.now());
        rentalItemRepository.save(item);

        // Act
        boolean existsRented = rentalItemRepository.existsByBikeIdAndStatus(BIKE_ID_1, RentalItemStatus.RENTED);
        boolean existsReturned = rentalItemRepository.existsByBikeIdAndStatus(BIKE_ID_1, RentalItemStatus.RETURNED);

        // Assert
        assertThat(existsRented).isFalse();
        assertThat(existsReturned).isTrue();
    }

    @Test
    void findByBikeIdAndStatus_ReturnsCorrectItem() {
        // Arrange
        RentalItem rentedItem = new RentalItem(testRental, BIKE_ID_1);
        rentedItem.setStatus(RentalItemStatus.RENTED);
        rentalItemRepository.save(rentedItem);

        // Act
        var result = rentalItemRepository.findByBikeIdAndStatus(BIKE_ID_1, RentalItemStatus.RENTED);

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().getBikeId()).isEqualTo(BIKE_ID_1);
    }

    @Test
    void countByRentalRentalIdAndStatus_ReturnsCorrectCounts() {
        // Arrange
        RentalItem item1 = new RentalItem(testRental, BIKE_ID_1);
        item1.setStatus(RentalItemStatus.RENTED);
        
        RentalItem item2 = new RentalItem(testRental, BIKE_ID_2);
        item2.setStatus(RentalItemStatus.RETURNED);
        item2.setReturnedAt(Instant.now());
        
        rentalItemRepository.save(item1);
        rentalItemRepository.save(item2);

        // Act
        long rentedCount = rentalItemRepository.countByRentalRentalIdAndStatus(
                testRental.getRentalId(), RentalItemStatus.RENTED);
        long returnedCount = rentalItemRepository.countByRentalRentalIdAndStatus(
                testRental.getRentalId(), RentalItemStatus.RETURNED);

        // Assert
        assertThat(rentedCount).isEqualTo(1);
        assertThat(returnedCount).isEqualTo(1);
    }

    @Test
    void findByRentalRentalIdAndStatus_ReturnsFilteredItems() {
        // Arrange
        RentalItem item1 = new RentalItem(testRental, BIKE_ID_1);
        item1.setStatus(RentalItemStatus.RENTED);
        
        RentalItem item2 = new RentalItem(testRental, BIKE_ID_2);
        item2.setStatus(RentalItemStatus.RETURNED);
        
        rentalItemRepository.save(item1);
        rentalItemRepository.save(item2);

        // Act
        List<RentalItem> rentedItems = rentalItemRepository.findByRentalRentalIdAndStatus(
                testRental.getRentalId(), RentalItemStatus.RENTED);

        // Assert
        assertThat(rentedItems).hasSize(1);
        assertThat(rentedItems.get(0).getBikeId()).isEqualTo(BIKE_ID_1);
    }

    // ==================== I1 Invariant Tests (Service-level check) ====================
    
    /**
     * Tests that the repository query correctly detects when a bike is already rented.
     * This is the service-level pre-check for I1 invariant enforcement.
     */
    @Test
    void i1_existsByBikeIdAndStatus_DetectsAlreadyRentedBike() {
        // Arrange - bike is rented in first rental
        RentalItem firstRental = new RentalItem(testRental, BIKE_ID_1);
        firstRental.setStatus(RentalItemStatus.RENTED);
        rentalItemRepository.save(firstRental);
        entityManager.flush();

        // Act - check if bike can be rented again (service-level I1 check)
        boolean isAlreadyRented = rentalItemRepository.existsByBikeIdAndStatus(
                BIKE_ID_1, RentalItemStatus.RENTED);

        // Assert
        assertThat(isAlreadyRented).isTrue();
        // In actual RentalService.createRental(), this would throw ConflictException
    }

    /**
     * Tests that a bike can be rented again after being returned.
     */
    @Test
    void i1_bikeCanBeRentedAgainAfterReturn() {
        // Arrange - bike was rented and returned
        RentalItem previousRental = new RentalItem(testRental, BIKE_ID_1);
        previousRental.setStatus(RentalItemStatus.RETURNED);
        previousRental.setReturnedAt(Instant.now());
        rentalItemRepository.save(previousRental);
        entityManager.flush();

        // Act - check if bike can be rented again
        boolean isCurrentlyRented = rentalItemRepository.existsByBikeIdAndStatus(
                BIKE_ID_1, RentalItemStatus.RENTED);

        // Assert - bike should be available for new rental
        assertThat(isCurrentlyRented).isFalse();
    }

    /**
     * Tests that multiple RETURNED/LOST items for same bike don't conflict.
     */
    @Test
    void i1_multiplePastRentalsForSameBikeAllowed() {
        // Arrange - bike has rental history (all completed)
        Rental rental1 = testRental;
        
        Rental rental2 = new Rental();
        rental2.setHotelId(HOTEL_ID);
        rental2.setStatus(RentalStatus.CLOSED);
        rental2.setStartAt(Instant.now().minus(48, ChronoUnit.HOURS));
        rental2.setDueAt(Instant.now().minus(24, ChronoUnit.HOURS));
        rental2.setReturnAt(Instant.now().minus(23, ChronoUnit.HOURS));
        rental2.setRoomNumber("102");
        rental2.setTncVersion("1.0");
        rental2.setSignatureId(SIGNATURE_ID);
        rental2 = rentalRepository.save(rental2);

        // Create multiple RETURNED items for same bike
        RentalItem item1 = new RentalItem(rental1, BIKE_ID_1);
        item1.setStatus(RentalItemStatus.RETURNED);
        item1.setReturnedAt(Instant.now().minus(10, ChronoUnit.HOURS));
        
        RentalItem item2 = new RentalItem(rental2, BIKE_ID_1);
        item2.setStatus(RentalItemStatus.RETURNED);
        item2.setReturnedAt(Instant.now().minus(23, ChronoUnit.HOURS));
        
        rentalItemRepository.save(item1);
        rentalItemRepository.save(item2);
        entityManager.flush();

        // Act
        List<RentalItem> bikeHistory = rentalItemRepository.findByBikeId(BIKE_ID_1);
        boolean isCurrentlyRented = rentalItemRepository.existsByBikeIdAndStatus(
                BIKE_ID_1, RentalItemStatus.RENTED);

        // Assert
        assertThat(bikeHistory).hasSize(2); // Multiple historical rentals OK
        assertThat(isCurrentlyRented).isFalse(); // But no current RENTED item
    }

    @Test
    void saveItem_WithLostStatus_StoresLostReason() {
        // Arrange
        RentalItem item = new RentalItem(testRental, BIKE_ID_1);
        item.setStatus(RentalItemStatus.LOST);
        item.setLostReason("Guest never returned");

        // Act
        RentalItem saved = rentalItemRepository.save(item);

        // Assert
        assertThat(saved.getStatus()).isEqualTo(RentalItemStatus.LOST);
        assertThat(saved.getLostReason()).isEqualTo("Guest never returned");
    }
}

