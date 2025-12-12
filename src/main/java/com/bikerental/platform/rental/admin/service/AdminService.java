package com.bikerental.platform.rental.admin.service;

import com.bikerental.platform.rental.admin.dto.CreateHotelRequest;
import com.bikerental.platform.rental.admin.dto.HotelResponse;
import com.bikerental.platform.rental.auth.model.Hotel;
import com.bikerental.platform.rental.auth.repo.HotelRepository;
import com.bikerental.platform.rental.common.exception.ConflictException;
import com.bikerental.platform.rental.common.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final HotelRepository hotelRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Create a new hotel with hashed password.
     */
    @Transactional
    public HotelResponse createHotel(CreateHotelRequest request) {
        if (hotelRepository.existsByHotelCode(request.getHotelCode())) {
            throw new ConflictException("Hotel code '" + request.getHotelCode() + "' already exists");
        }

        Hotel hotel = new Hotel();
        hotel.setHotelCode(request.getHotelCode());
        hotel.setHotelName(request.getHotelName());
        hotel.setPasswordHash(passwordEncoder.encode(request.getPassword()));

        return toResponse(hotelRepository.save(hotel));
    }

    /**
     * Reset a hotel's password.
     */
    @Transactional
    public void resetPassword(Long hotelId, String newPassword) {
        Hotel hotel = hotelRepository.findById(hotelId)
                .orElseThrow(() -> new NotFoundException("Hotel not found with ID: " + hotelId));

        hotel.setPasswordHash(passwordEncoder.encode(newPassword));
        hotelRepository.save(hotel);
    }

    /**
     * List all hotels (useful to find hotel ID for password reset).
     */
    public List<HotelResponse> getAllHotels() {
        return hotelRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    private HotelResponse toResponse(Hotel hotel) {
        return new HotelResponse(
                hotel.getHotelId(),
                hotel.getHotelCode(),
                hotel.getHotelName(),
                hotel.getCreatedAt()
        );
    }
}
