package com.bikerental.platform.rental.bike.service;

import com.bikerental.platform.rental.auth.security.HotelContext;
import com.bikerental.platform.rental.bike.model.Bike;
import com.bikerental.platform.rental.bike.repo.BikeRepository;
import com.bikerental.platform.rental.common.exception.ConflictException;
import com.bikerental.platform.rental.common.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BikeService {

    private final BikeRepository bikeRepository;
    private final HotelContext hotelContext;

    /**
     * List bikes for the current hotel, optionally filtered by status and search query.
     */
    public List<Bike> listBikes(Bike.BikeStatus status, String searchQuery) {
        Long hotelId = hotelContext.getCurrentHotelId();
        return bikeRepository.findByHotelIdWithFilters(hotelId, status, searchQuery);
    }

    /**
     * Find a bike by bike number within the current hotel.
     */
    public Bike findByBikeNumber(String bikeNumber) {
        Long hotelId = hotelContext.getCurrentHotelId();
        return bikeRepository.findByHotelIdAndBikeNumber(hotelId, bikeNumber)
                .orElseThrow(() -> new NotFoundException("Bike not found: " + bikeNumber));
    }

    /**
     * Find a bike by ID, ensuring it belongs to the current hotel.
     */
    public Bike findById(Long bikeId) {
        Long hotelId = hotelContext.getCurrentHotelId();
        return bikeRepository.findById(bikeId)
                .filter(bike -> bike.getHotelId().equals(hotelId))
                .orElseThrow(() -> new NotFoundException("Bike not found: " + bikeId));
    }

    /**
     * Mark a bike as Out of Order (OOO).
     * Sets status to OOO, stores the note, and records the timestamp.
     */
    @Transactional
    public Bike markOoo(Long bikeId, String note) {
        Bike bike = findById(bikeId);
        bike.setStatus(Bike.BikeStatus.OOO);
        bike.setOooNote(note);
        bike.setOooSince(Instant.now());
        return bikeRepository.save(bike);
    }

    /**
     * Mark a bike as available.
     * Clears OOO fields and sets status to AVAILABLE.
     * Fails if the bike is currently RENTED (part of an active rental).
     */
    @Transactional
    public Bike markAvailable(Long bikeId) {
        Bike bike = findById(bikeId);
        
        if (bike.getStatus() == Bike.BikeStatus.RENTED) {
            throw new ConflictException("Cannot mark bike as available: bike is currently rented");
        }
        
        bike.setStatus(Bike.BikeStatus.AVAILABLE);
        bike.setOooNote(null);
        bike.setOooSince(null);
        return bikeRepository.save(bike);
    }
}

