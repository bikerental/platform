package com.bikerental.platform.rental.settings.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.bikerental.platform.rental.settings.model.HotelSettings;
import com.bikerental.platform.rental.settings.repo.HotelSettingsRepository;

/**
 * Unit tests for HotelSettingsService.
 * Tests fallback to defaults when settings are null/missing.
 */
@ExtendWith(MockitoExtension.class)
class HotelSettingsServiceTest {

    @Mock
    private HotelSettingsRepository hotelSettingsRepository;

    @InjectMocks
    private HotelSettingsService hotelSettingsService;

    private static final Long HOTEL_ID = 1L;

    private HotelSettings testSettings;

    @BeforeEach
    void setUp() {
        testSettings = new HotelSettings(HOTEL_ID);
        testSettings.setSettingsId(100L);
        testSettings.setGraceMinutes(30);
        testSettings.setTncText("Custom T&C text for testing");
        testSettings.setTncVersion("2.5");
        testSettings.setRentalDurationOptions("[12, 24, 36]");
    }

    // ==================== getSettings Tests ====================

    @Test
    void getSettings_WhenExists_ReturnsSettings() {
        // Arrange
        when(hotelSettingsRepository.findByHotelId(HOTEL_ID)).thenReturn(Optional.of(testSettings));

        // Act
        Optional<HotelSettings> result = hotelSettingsService.getSettings(HOTEL_ID);

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().getGraceMinutes()).isEqualTo(30);
    }

    @Test
    void getSettings_WhenNotExists_ReturnsEmpty() {
        // Arrange
        when(hotelSettingsRepository.findByHotelId(HOTEL_ID)).thenReturn(Optional.empty());

        // Act
        Optional<HotelSettings> result = hotelSettingsService.getSettings(HOTEL_ID);

        // Assert
        assertThat(result).isEmpty();
    }

    // ==================== getGraceMinutes Tests ====================

    @Test
    void getGraceMinutes_WhenSettingsExist_ReturnsConfiguredValue() {
        // Arrange
        when(hotelSettingsRepository.findByHotelId(HOTEL_ID)).thenReturn(Optional.of(testSettings));

        // Act
        int result = hotelSettingsService.getGraceMinutes(HOTEL_ID);

        // Assert
        assertThat(result).isEqualTo(30);
    }

    @Test
    void getGraceMinutes_WhenSettingsNull_ReturnsDefaultZero() {
        // Arrange
        when(hotelSettingsRepository.findByHotelId(HOTEL_ID)).thenReturn(Optional.empty());

        // Act
        int result = hotelSettingsService.getGraceMinutes(HOTEL_ID);

        // Assert
        assertThat(result).isEqualTo(0);
    }

    // ==================== getTncText Tests ====================

    @Test
    void getTncText_WhenSettingsExist_ReturnsConfiguredText() {
        // Arrange
        when(hotelSettingsRepository.findByHotelId(HOTEL_ID)).thenReturn(Optional.of(testSettings));

        // Act
        String result = hotelSettingsService.getTncText(HOTEL_ID);

        // Assert
        assertThat(result).isEqualTo("Custom T&C text for testing");
    }

    @Test
    void getTncText_WhenSettingsNull_ReturnsDefaultText() {
        // Arrange
        when(hotelSettingsRepository.findByHotelId(HOTEL_ID)).thenReturn(Optional.empty());

        // Act
        String result = hotelSettingsService.getTncText(HOTEL_ID);

        // Assert
        assertThat(result).contains("By signing below");
        assertThat(result).contains("bicycle");
    }

    @Test
    void getTncText_WhenTextBlank_ReturnsDefaultText() {
        // Arrange
        testSettings.setTncText("   ");
        when(hotelSettingsRepository.findByHotelId(HOTEL_ID)).thenReturn(Optional.of(testSettings));

        // Act
        String result = hotelSettingsService.getTncText(HOTEL_ID);

        // Assert
        assertThat(result).contains("By signing below");
    }

    @Test
    void getTncText_WhenTextNull_ReturnsDefaultText() {
        // Arrange
        testSettings.setTncText(null);
        when(hotelSettingsRepository.findByHotelId(HOTEL_ID)).thenReturn(Optional.of(testSettings));

        // Act
        String result = hotelSettingsService.getTncText(HOTEL_ID);

        // Assert
        assertThat(result).contains("By signing below");
    }

    // ==================== getTncVersion Tests ====================

    @Test
    void getTncVersion_WhenSettingsExist_ReturnsConfiguredVersion() {
        // Arrange
        when(hotelSettingsRepository.findByHotelId(HOTEL_ID)).thenReturn(Optional.of(testSettings));

        // Act
        String result = hotelSettingsService.getTncVersion(HOTEL_ID);

        // Assert
        assertThat(result).isEqualTo("2.5");
    }

    @Test
    void getTncVersion_WhenSettingsNull_ReturnsDefault() {
        // Arrange
        when(hotelSettingsRepository.findByHotelId(HOTEL_ID)).thenReturn(Optional.empty());

        // Act
        String result = hotelSettingsService.getTncVersion(HOTEL_ID);

        // Assert
        assertThat(result).isEqualTo("1.0");
    }

    @Test
    void getTncVersion_WhenVersionBlank_ReturnsDefault() {
        // Arrange
        testSettings.setTncVersion("");
        when(hotelSettingsRepository.findByHotelId(HOTEL_ID)).thenReturn(Optional.of(testSettings));

        // Act
        String result = hotelSettingsService.getTncVersion(HOTEL_ID);

        // Assert
        assertThat(result).isEqualTo("1.0");
    }

    // ==================== getRentalDurationOptions Tests ====================

    @Test
    void getRentalDurationOptions_WhenSettingsExist_ReturnsConfiguredOptions() {
        // Arrange
        when(hotelSettingsRepository.findByHotelId(HOTEL_ID)).thenReturn(Optional.of(testSettings));

        // Act
        List<Integer> result = hotelSettingsService.getRentalDurationOptions(HOTEL_ID);

        // Assert
        assertThat(result).containsExactly(12, 24, 36);
    }

    @Test
    void getRentalDurationOptions_WhenSettingsNull_ReturnsDefault() {
        // Arrange
        when(hotelSettingsRepository.findByHotelId(HOTEL_ID)).thenReturn(Optional.empty());

        // Act
        List<Integer> result = hotelSettingsService.getRentalDurationOptions(HOTEL_ID);

        // Assert
        assertThat(result).containsExactly(24, 48, 72);
    }

    @Test
    void getRentalDurationOptions_WhenJsonEmpty_ReturnsDefault() {
        // Arrange
        testSettings.setRentalDurationOptions("");
        when(hotelSettingsRepository.findByHotelId(HOTEL_ID)).thenReturn(Optional.of(testSettings));

        // Act
        List<Integer> result = hotelSettingsService.getRentalDurationOptions(HOTEL_ID);

        // Assert
        assertThat(result).containsExactly(24, 48, 72);
    }

    @Test
    void getRentalDurationOptions_WhenJsonInvalid_ReturnsDefault() {
        // Arrange
        testSettings.setRentalDurationOptions("[not, valid, json]");
        when(hotelSettingsRepository.findByHotelId(HOTEL_ID)).thenReturn(Optional.of(testSettings));

        // Act
        List<Integer> result = hotelSettingsService.getRentalDurationOptions(HOTEL_ID);

        // Assert
        assertThat(result).containsExactly(24, 48, 72);
    }

    @Test
    void getRentalDurationOptions_ParsesJsonWithSpaces() {
        // Arrange
        testSettings.setRentalDurationOptions("[ 6 , 12 , 24 ]");
        when(hotelSettingsRepository.findByHotelId(HOTEL_ID)).thenReturn(Optional.of(testSettings));

        // Act
        List<Integer> result = hotelSettingsService.getRentalDurationOptions(HOTEL_ID);

        // Assert
        assertThat(result).containsExactly(6, 12, 24);
    }

    @Test
    void getRentalDurationOptions_ParsesJsonWithoutBrackets() {
        // Arrange - test that we handle edge case
        testSettings.setRentalDurationOptions("48,72,96");
        when(hotelSettingsRepository.findByHotelId(HOTEL_ID)).thenReturn(Optional.of(testSettings));

        // Act
        List<Integer> result = hotelSettingsService.getRentalDurationOptions(HOTEL_ID);

        // Assert
        assertThat(result).containsExactly(48, 72, 96);
    }
}

