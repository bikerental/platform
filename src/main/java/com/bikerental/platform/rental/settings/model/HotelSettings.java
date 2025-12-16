package com.bikerental.platform.rental.settings.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Hotel-specific settings for rental configuration.
 * One settings record per hotel (unique constraint on hotel_id).
 * For MVP, service layer provides defaults when settings are null/missing.
 */
@Entity
@Table(name = "hotel_settings", uniqueConstraints = {
    @UniqueConstraint(name = "uk_hotel_settings", columnNames = {"hotel_id"})
})
@Getter
@Setter
@NoArgsConstructor
public class HotelSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "settings_id")
    private Long settingsId;

    @Column(name = "hotel_id", nullable = false)
    private Long hotelId;

    /**
     * JSON array of allowed rental duration options in hours.
     * Example: "[24, 48, 72]"
     */
    @Column(name = "rental_duration_options", columnDefinition = "TEXT")
    private String rentalDurationOptions;

    /**
     * Grace period in minutes before rental becomes OVERDUE.
     * Default: 0
     */
    @Column(name = "grace_minutes", nullable = false)
    private Integer graceMinutes = 0;

    /**
     * Terms & Conditions text displayed during rental creation.
     */
    @Column(name = "tnc_text", columnDefinition = "TEXT")
    private String tncText;

    /**
     * Version string for T&C (e.g., ISO timestamp or incrementing version).
     * Stored with each rental to track which T&C version was accepted.
     */
    @Column(name = "tnc_version", length = 50)
    private String tncVersion;

    public HotelSettings(Long hotelId) {
        this.hotelId = hotelId;
        this.graceMinutes = 0;
    }
}

