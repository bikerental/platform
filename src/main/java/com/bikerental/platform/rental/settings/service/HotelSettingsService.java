package com.bikerental.platform.rental.settings.service;

import com.bikerental.platform.rental.settings.model.HotelSettings;
import com.bikerental.platform.rental.settings.repo.HotelSettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

// Hotel config with sensible defaults - grace period, T&C, rental durations
@Service
@RequiredArgsConstructor
public class HotelSettingsService {

    private final HotelSettingsRepository hotelSettingsRepository;

    private static final int DEFAULT_GRACE_MINUTES = 0;
    private static final String DEFAULT_TNC_VERSION = "1.0";
    private static final String DEFAULT_TNC_TEXT = "By signing below, I acknowledge that I have received the bicycle(s) listed above in good condition. " +
            "I agree to return them by the specified due date and time. " +
            "I accept responsibility for any damage to or loss of the bicycle(s) during the rental period. " +
            "I understand that late returns may incur additional charges.";
    private static final List<Integer> DEFAULT_RENTAL_DURATION_OPTIONS = Arrays.asList(24, 48, 72);

    public Optional<HotelSettings> getSettings(Long hotelId) {
        return hotelSettingsRepository.findByHotelId(hotelId);
    }

    public int getGraceMinutes(Long hotelId) {
        return hotelSettingsRepository.findByHotelId(hotelId)
                .map(HotelSettings::getGraceMinutes)
                .map(minutes -> minutes != null ? minutes : DEFAULT_GRACE_MINUTES)
                .orElse(DEFAULT_GRACE_MINUTES);
    }

    public String getTncText(Long hotelId) {
        return hotelSettingsRepository.findByHotelId(hotelId)
                .map(HotelSettings::getTncText)
                .filter(text -> text != null && !text.isBlank())
                .orElse(DEFAULT_TNC_TEXT);
    }

    public String getTncVersion(Long hotelId) {
        return hotelSettingsRepository.findByHotelId(hotelId)
                .map(HotelSettings::getTncVersion)
                .filter(version -> version != null && !version.isBlank())
                .orElse(DEFAULT_TNC_VERSION);
    }

    public List<Integer> getRentalDurationOptions(Long hotelId) {
        return hotelSettingsRepository.findByHotelId(hotelId)
                .map(HotelSettings::getRentalDurationOptions)
                .filter(json -> json != null && !json.isBlank())
                .map(this::parseRentalDurationOptions)
                .orElse(DEFAULT_RENTAL_DURATION_OPTIONS);
    }

    private List<Integer> parseRentalDurationOptions(String json) {
        try {
            // Simple parsing for JSON array of integers
            String cleaned = json.replaceAll("[\\[\\]\\s]", "");
            if (cleaned.isEmpty()) {
                return DEFAULT_RENTAL_DURATION_OPTIONS;
            }
            return Arrays.stream(cleaned.split(","))
                    .map(String::trim)
                    .map(Integer::parseInt)
                    .toList();
        } catch (NumberFormatException e) {
            return DEFAULT_RENTAL_DURATION_OPTIONS;
        }
    }
}

