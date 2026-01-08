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

// Bike inventory management - handles status transitions and filtering
@Service
@RequiredArgsConstructor
public class BikeService {

    private final BikeRepository bikeRepository;
    private final HotelContext hotelContext;

    // OOO bikes use different sorting (oldest first) to prioritize maintenance
    public List<Bike> listBikes(Bike.BikeStatus status, String searchQuery) {
        Long hotelId = hotelContext.getCurrentHotelId();

        if (status == Bike.BikeStatus.OOO) {
            return bikeRepository.findOooBikesWithFilters(hotelId, searchQuery);
        }

        return bikeRepository.findByHotelIdWithFilters(hotelId, status != null ? status.name() : null, searchQuery);
    }

    public Bike findByBikeNumber(String bikeNumber) {
        Long hotelId = hotelContext.getCurrentHotelId();
        return bikeRepository.findByHotelIdAndBikeNumber(hotelId, bikeNumber)
                .orElseThrow(() -> new NotFoundException("Bike not found: " + bikeNumber));
    }

    public Bike findById(Long bikeId) {
        Long hotelId = hotelContext.getCurrentHotelId();
        return bikeRepository.findById(bikeId)
                .filter(bike -> bike.getHotelId().equals(hotelId))
                .orElseThrow(() -> new NotFoundException("Bike not found: " + bikeId));
    }

    @Transactional
    public Bike markOoo(Long bikeId, String note) {
        Bike bike = findById(bikeId);
        bike.setStatus(Bike.BikeStatus.OOO);
        bike.setOooNote(note);
        bike.setOooSince(Instant.now());
        return bikeRepository.save(bike);
    }

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

