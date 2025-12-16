package com.bikerental.platform.rental.bike.repo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import com.bikerental.platform.rental.bike.model.Bike;

/**
 * Integration test for BikeRepository.
 * Tests the unique constraint on (hotel_id, bike_number).
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
class BikeRepositoryIntegrationTest {

    @Autowired
    private BikeRepository bikeRepository;

    private static final Long HOTEL_ID_1 = 1L;
    private static final Long HOTEL_ID_2 = 2L;
    private static final String BIKE_NUMBER_1 = "B001";

    @BeforeEach
    void setUp() {
        bikeRepository.deleteAll();
    }

    @Test
    void saveBike_WithUniqueHotelIdAndBikeNumber_Succeeds() {
        // Arrange
        Bike bike1 = new Bike();
        bike1.setHotelId(HOTEL_ID_1);
        bike1.setBikeNumber(BIKE_NUMBER_1);
        bike1.setStatus(Bike.BikeStatus.AVAILABLE);

        // Act
        Bike saved = bikeRepository.save(bike1);

        // Assert
        assertThat(saved.getBikeId()).isNotNull();
        assertThat(bikeRepository.count()).isEqualTo(1);
    }

    @Test
    void saveBike_WithSameBikeNumberDifferentHotel_Succeeds() {
        // Arrange
        Bike bike1 = new Bike();
        bike1.setHotelId(HOTEL_ID_1);
        bike1.setBikeNumber(BIKE_NUMBER_1);
        bike1.setStatus(Bike.BikeStatus.AVAILABLE);
        bikeRepository.save(bike1);

        Bike bike2 = new Bike();
        bike2.setHotelId(HOTEL_ID_2);
        bike2.setBikeNumber(BIKE_NUMBER_1); // Same bike number, different hotel
        bike2.setStatus(Bike.BikeStatus.AVAILABLE);

        // Act
        Bike saved = bikeRepository.save(bike2);

        // Assert
        assertThat(saved.getBikeId()).isNotNull();
        assertThat(bikeRepository.count()).isEqualTo(2);
    }

    @Test
    void saveBike_WithDuplicateHotelIdAndBikeNumber_ThrowsException() {
        // Arrange
        Bike bike1 = new Bike();
        bike1.setHotelId(HOTEL_ID_1);
        bike1.setBikeNumber(BIKE_NUMBER_1);
        bike1.setStatus(Bike.BikeStatus.AVAILABLE);
        bikeRepository.saveAndFlush(bike1);

        Bike bike2 = new Bike();
        bike2.setHotelId(HOTEL_ID_1); // Same hotel
        bike2.setBikeNumber(BIKE_NUMBER_1); // Same bike number
        bike2.setStatus(Bike.BikeStatus.AVAILABLE);

        // Act & Assert
        assertThatThrownBy(() -> bikeRepository.saveAndFlush(bike2))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void findByHotelIdAndBikeNumber_ReturnsCorrectBike() {
        // Arrange
        Bike bike1 = new Bike();
        bike1.setHotelId(HOTEL_ID_1);
        bike1.setBikeNumber(BIKE_NUMBER_1);
        bike1.setStatus(Bike.BikeStatus.AVAILABLE);
        bikeRepository.save(bike1);

        Bike bike2 = new Bike();
        bike2.setHotelId(HOTEL_ID_1);
        bike2.setBikeNumber("B002");
        bike2.setStatus(Bike.BikeStatus.AVAILABLE);
        bikeRepository.save(bike2);

        // Act
        var result = bikeRepository.findByHotelIdAndBikeNumber(HOTEL_ID_1, BIKE_NUMBER_1);

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().getBikeNumber()).isEqualTo(BIKE_NUMBER_1);
    }

    @Test
    void findByHotelIdAndBikeNumber_WithWrongHotel_ReturnsEmpty() {
        // Arrange
        Bike bike1 = new Bike();
        bike1.setHotelId(HOTEL_ID_1);
        bike1.setBikeNumber(BIKE_NUMBER_1);
        bike1.setStatus(Bike.BikeStatus.AVAILABLE);
        bikeRepository.save(bike1);

        // Act
        var result = bikeRepository.findByHotelIdAndBikeNumber(HOTEL_ID_2, BIKE_NUMBER_1);

        // Assert
        assertThat(result).isEmpty();
    }
}

