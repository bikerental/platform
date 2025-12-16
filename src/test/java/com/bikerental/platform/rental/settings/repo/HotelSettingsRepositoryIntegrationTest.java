package com.bikerental.platform.rental.settings.repo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import com.bikerental.platform.rental.settings.model.HotelSettings;

/**
 * Integration tests for HotelSettingsRepository.
 * Tests unique constraint on hotel_id and basic CRUD operations.
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
class HotelSettingsRepositoryIntegrationTest {

    @Autowired
    private HotelSettingsRepository hotelSettingsRepository;

    private static final Long HOTEL_ID_1 = 1L;
    private static final Long HOTEL_ID_2 = 2L;

    @BeforeEach
    void setUp() {
        hotelSettingsRepository.deleteAll();
    }

    @Test
    void saveSettings_WithValidData_Succeeds() {
        // Arrange
        HotelSettings settings = new HotelSettings(HOTEL_ID_1);
        settings.setGraceMinutes(15);
        settings.setTncText("Test T&C");
        settings.setTncVersion("2.0");
        settings.setRentalDurationOptions("[24, 48]");

        // Act
        HotelSettings saved = hotelSettingsRepository.save(settings);

        // Assert
        assertThat(saved.getSettingsId()).isNotNull();
        assertThat(saved.getHotelId()).isEqualTo(HOTEL_ID_1);
        assertThat(saved.getGraceMinutes()).isEqualTo(15);
    }

    @Test
    void findByHotelId_WhenExists_ReturnsSettings() {
        // Arrange
        HotelSettings settings = new HotelSettings(HOTEL_ID_1);
        settings.setGraceMinutes(30);
        hotelSettingsRepository.save(settings);

        // Act
        var result = hotelSettingsRepository.findByHotelId(HOTEL_ID_1);

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().getGraceMinutes()).isEqualTo(30);
    }

    @Test
    void findByHotelId_WhenNotExists_ReturnsEmpty() {
        // Act
        var result = hotelSettingsRepository.findByHotelId(HOTEL_ID_1);

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    void existsByHotelId_WhenExists_ReturnsTrue() {
        // Arrange
        hotelSettingsRepository.save(new HotelSettings(HOTEL_ID_1));

        // Act & Assert
        assertThat(hotelSettingsRepository.existsByHotelId(HOTEL_ID_1)).isTrue();
        assertThat(hotelSettingsRepository.existsByHotelId(HOTEL_ID_2)).isFalse();
    }

    @Test
    void saveSettings_DuplicateHotelId_ThrowsException() {
        // Arrange
        HotelSettings settings1 = new HotelSettings(HOTEL_ID_1);
        hotelSettingsRepository.saveAndFlush(settings1);

        HotelSettings settings2 = new HotelSettings(HOTEL_ID_1); // Same hotel ID

        // Act & Assert
        assertThatThrownBy(() -> hotelSettingsRepository.saveAndFlush(settings2))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void saveSettings_DifferentHotels_Succeeds() {
        // Arrange
        HotelSettings settings1 = new HotelSettings(HOTEL_ID_1);
        HotelSettings settings2 = new HotelSettings(HOTEL_ID_2);

        // Act
        hotelSettingsRepository.save(settings1);
        hotelSettingsRepository.save(settings2);

        // Assert
        assertThat(hotelSettingsRepository.count()).isEqualTo(2);
    }

    @Test
    void saveSettings_WithNullOptionalFields_Succeeds() {
        // Arrange
        HotelSettings settings = new HotelSettings(HOTEL_ID_1);
        // Don't set optional fields (tncText, tncVersion, rentalDurationOptions)

        // Act
        HotelSettings saved = hotelSettingsRepository.save(settings);

        // Assert
        assertThat(saved.getSettingsId()).isNotNull();
        assertThat(saved.getTncText()).isNull();
        assertThat(saved.getTncVersion()).isNull();
        assertThat(saved.getRentalDurationOptions()).isNull();
    }

    @Test
    void updateSettings_ModifiesExisting() {
        // Arrange
        HotelSettings settings = new HotelSettings(HOTEL_ID_1);
        settings.setGraceMinutes(0);
        HotelSettings saved = hotelSettingsRepository.save(settings);

        // Act
        saved.setGraceMinutes(60);
        saved.setTncVersion("3.0");
        hotelSettingsRepository.save(saved);

        // Assert
        var updated = hotelSettingsRepository.findByHotelId(HOTEL_ID_1);
        assertThat(updated).isPresent();
        assertThat(updated.get().getGraceMinutes()).isEqualTo(60);
        assertThat(updated.get().getTncVersion()).isEqualTo("3.0");
    }
}

