package com.bikerental.platform.rental.rentals.repo;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import com.bikerental.platform.rental.rentals.model.Rental;
import com.bikerental.platform.rental.rentals.model.RentalStatus;

/**
 * Integration tests for RentalRepository.
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
class RentalRepositoryIntegrationTest {

    @Autowired
    private RentalRepository rentalRepository;

    private static final Long HOTEL_ID_1 = 1L;
    private static final Long HOTEL_ID_2 = 2L;
    private static final Long SIGNATURE_ID = 100L;

    @BeforeEach
    void setUp() {
        rentalRepository.deleteAll();
    }

    private Rental createRental(Long hotelId, RentalStatus status, String roomNumber) {
        Rental rental = new Rental();
        rental.setHotelId(hotelId);
        rental.setStatus(status);
        rental.setStartAt(Instant.now());
        rental.setDueAt(Instant.now().plus(24, ChronoUnit.HOURS));
        rental.setRoomNumber(roomNumber);
        rental.setTncVersion("1.0");
        rental.setSignatureId(SIGNATURE_ID);
        return rental;
    }

    @Test
    void saveRental_WithValidData_Succeeds() {
        // Arrange
        Rental rental = createRental(HOTEL_ID_1, RentalStatus.ACTIVE, "101");

        // Act
        Rental saved = rentalRepository.save(rental);

        // Assert
        assertThat(saved.getRentalId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getStatus()).isEqualTo(RentalStatus.ACTIVE);
    }

    @Test
    void findByHotelId_ReturnsOnlyRentalsForThatHotel() {
        // Arrange
        rentalRepository.save(createRental(HOTEL_ID_1, RentalStatus.ACTIVE, "101"));
        rentalRepository.save(createRental(HOTEL_ID_1, RentalStatus.CLOSED, "102"));
        rentalRepository.save(createRental(HOTEL_ID_2, RentalStatus.ACTIVE, "201"));

        // Act
        List<Rental> result = rentalRepository.findByHotelId(HOTEL_ID_1);

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result).allMatch(r -> r.getHotelId().equals(HOTEL_ID_1));
    }

    @Test
    void findByHotelIdAndStatus_ReturnsFilteredRentals() {
        // Arrange
        rentalRepository.save(createRental(HOTEL_ID_1, RentalStatus.ACTIVE, "101"));
        rentalRepository.save(createRental(HOTEL_ID_1, RentalStatus.ACTIVE, "102"));
        rentalRepository.save(createRental(HOTEL_ID_1, RentalStatus.CLOSED, "103"));

        // Act
        List<Rental> result = rentalRepository.findByHotelIdAndStatus(HOTEL_ID_1, RentalStatus.ACTIVE);

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result).allMatch(r -> r.getStatus() == RentalStatus.ACTIVE);
    }

    @Test
    void findByHotelIdAndStatusIn_ReturnsMultipleStatuses() {
        // Arrange
        rentalRepository.save(createRental(HOTEL_ID_1, RentalStatus.ACTIVE, "101"));
        rentalRepository.save(createRental(HOTEL_ID_1, RentalStatus.OVERDUE, "102"));
        rentalRepository.save(createRental(HOTEL_ID_1, RentalStatus.CLOSED, "103"));

        // Act
        List<Rental> result = rentalRepository.findByHotelIdAndStatusIn(
                HOTEL_ID_1, 
                Arrays.asList(RentalStatus.ACTIVE, RentalStatus.OVERDUE)
        );

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result).noneMatch(r -> r.getStatus() == RentalStatus.CLOSED);
    }

    @Test
    void findByRentalIdAndHotelId_EnforcesHotelScoping() {
        // Arrange
        Rental saved = rentalRepository.save(createRental(HOTEL_ID_1, RentalStatus.ACTIVE, "101"));

        // Act - correct hotel
        var result1 = rentalRepository.findByRentalIdAndHotelId(saved.getRentalId(), HOTEL_ID_1);
        // Act - wrong hotel
        var result2 = rentalRepository.findByRentalIdAndHotelId(saved.getRentalId(), HOTEL_ID_2);

        // Assert
        assertThat(result1).isPresent();
        assertThat(result2).isEmpty();
    }

    @Test
    void countByHotelIdAndStatus_ReturnsCorrectCount() {
        // Arrange
        rentalRepository.save(createRental(HOTEL_ID_1, RentalStatus.ACTIVE, "101"));
        rentalRepository.save(createRental(HOTEL_ID_1, RentalStatus.ACTIVE, "102"));
        rentalRepository.save(createRental(HOTEL_ID_1, RentalStatus.OVERDUE, "103"));

        // Act
        long activeCount = rentalRepository.countByHotelIdAndStatus(HOTEL_ID_1, RentalStatus.ACTIVE);
        long overdueCount = rentalRepository.countByHotelIdAndStatus(HOTEL_ID_1, RentalStatus.OVERDUE);

        // Assert
        assertThat(activeCount).isEqualTo(2);
        assertThat(overdueCount).isEqualTo(1);
    }

    @Test
    void findActiveAndOverdueOrderedByUrgency_SortsOverdueFirst() {
        // Arrange
        Rental active1 = createRental(HOTEL_ID_1, RentalStatus.ACTIVE, "101");
        active1.setDueAt(Instant.now().plus(48, ChronoUnit.HOURS));
        
        Rental active2 = createRental(HOTEL_ID_1, RentalStatus.ACTIVE, "102");
        active2.setDueAt(Instant.now().plus(24, ChronoUnit.HOURS));
        
        Rental overdue = createRental(HOTEL_ID_1, RentalStatus.OVERDUE, "103");
        overdue.setDueAt(Instant.now().minus(1, ChronoUnit.HOURS));
        
        rentalRepository.save(active1);
        rentalRepository.save(active2);
        rentalRepository.save(overdue);

        // Act
        List<Rental> result = rentalRepository.findActiveAndOverdueOrderedByUrgency(
                HOTEL_ID_1,
                Arrays.asList(RentalStatus.ACTIVE, RentalStatus.OVERDUE)
        );

        // Assert
        assertThat(result).hasSize(3);
        assertThat(result.get(0).getStatus()).isEqualTo(RentalStatus.OVERDUE);
        // Active rentals sorted by dueAt ascending
        assertThat(result.get(1).getRoomNumber()).isEqualTo("102"); // due in 24h
        assertThat(result.get(2).getRoomNumber()).isEqualTo("101"); // due in 48h
    }

    @Test
    void saveRental_WithOptionalBedNumber_Succeeds() {
        // Arrange
        Rental rental = createRental(HOTEL_ID_1, RentalStatus.ACTIVE, "101");
        rental.setBedNumber("A");

        // Act
        Rental saved = rentalRepository.save(rental);

        // Assert
        assertThat(saved.getBedNumber()).isEqualTo("A");
    }

    @Test
    void saveRental_WithNullBedNumber_Succeeds() {
        // Arrange
        Rental rental = createRental(HOTEL_ID_1, RentalStatus.ACTIVE, "101");
        rental.setBedNumber(null);

        // Act
        Rental saved = rentalRepository.save(rental);

        // Assert
        assertThat(saved.getBedNumber()).isNull();
    }
}

