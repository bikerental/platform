package com.bikerental.platform.rental.settings.repo;

import com.bikerental.platform.rental.settings.model.HotelSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for HotelSettings entities.
 */
@Repository
public interface HotelSettingsRepository extends JpaRepository<HotelSettings, Long> {

    /**
     * Find settings for a specific hotel.
     */
    Optional<HotelSettings> findByHotelId(Long hotelId);

    /**
     * Check if settings exist for a hotel.
     */
    boolean existsByHotelId(Long hotelId);
}

