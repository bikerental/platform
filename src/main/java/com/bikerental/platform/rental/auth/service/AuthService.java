package com.bikerental.platform.rental.auth.service;

import com.bikerental.platform.rental.auth.dto.LoginRequest;
import com.bikerental.platform.rental.auth.dto.LoginResponse;
import com.bikerental.platform.rental.auth.model.Hotel;
import com.bikerental.platform.rental.auth.repo.HotelRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AuthService {

    private final HotelRepository hotelRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
            HotelRepository hotelRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService) {
        this.hotelRepository = hotelRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    /**
     * Authenticate credentials and return JWT.
     * Supports both hotel login and admin login (is_admin=true in database).
     * Returns empty if credentials are invalid (generic error, no hints).
     */
    public Optional<LoginResponse> authenticate(LoginRequest request) {
        Optional<Hotel> hotelOpt = hotelRepository.findByHotelCode(request.getHotelCode());

        if (hotelOpt.isEmpty()) {
            return Optional.empty();
        }

        Hotel hotel = hotelOpt.get();

        if (!passwordEncoder.matches(request.getPassword(), hotel.getPasswordHash())) {
            return Optional.empty();
        }

        // Generate appropriate token based on admin status
        if (hotel.isAdmin()) {
            String token = jwtService.generateAdminToken(hotel.getHotelCode());
            return Optional.of(new LoginResponse(token, "System Administrator"));
        }

        String token = jwtService.generateToken(hotel.getHotelId(), hotel.getHotelCode());
        return Optional.of(new LoginResponse(token, hotel.getHotelName()));
    }
}
