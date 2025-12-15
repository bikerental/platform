package com.bikerental.platform.rental.auth.service;

import com.bikerental.platform.rental.auth.dto.LoginRequest;
import com.bikerental.platform.rental.auth.dto.LoginResponse;
import com.bikerental.platform.rental.auth.model.Hotel;
import com.bikerental.platform.rental.auth.repo.HotelRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AuthService {

    private final HotelRepository hotelRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final String adminUsername;
    private final String adminPasswordHash;

    public AuthService(
            HotelRepository hotelRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            @Value("${admin.username}") String adminUsername,
            @Value("${admin.password-hash}") String adminPasswordHash) {
        this.hotelRepository = hotelRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.adminUsername = adminUsername;
        this.adminPasswordHash = adminPasswordHash;
    }

    /**
     * Authenticate credentials and return JWT.
     * Supports both hotel login and admin login.
     * Returns empty if credentials are invalid (generic error, no hints).
     */
    public Optional<LoginResponse> authenticate(LoginRequest request) {
        // Check if this is an admin login
        if (adminUsername.equals(request.getHotelCode())) {
            return authenticateAdmin(request);
        }

        // Otherwise, authenticate as hotel
        return authenticateHotel(request);
    }

    private Optional<LoginResponse> authenticateAdmin(LoginRequest request) {
        if (!passwordEncoder.matches(request.getPassword(), adminPasswordHash)) {
            return Optional.empty();
        }

        String token = jwtService.generateAdminToken(adminUsername);
        return Optional.of(new LoginResponse(token, "System Administrator"));
    }

    private Optional<LoginResponse> authenticateHotel(LoginRequest request) {
        Optional<Hotel> hotelOpt = hotelRepository.findByHotelCode(request.getHotelCode());

        if (hotelOpt.isEmpty()) {
            return Optional.empty(); // Hotel not found - generic error
        }

        Hotel hotel = hotelOpt.get();

        if (!passwordEncoder.matches(request.getPassword(), hotel.getPasswordHash())) {
            return Optional.empty(); // Wrong password - generic error
        }

        String token = jwtService.generateToken(hotel.getHotelId(), hotel.getHotelCode());

        return Optional.of(new LoginResponse(token, hotel.getHotelName()));
    }
}
