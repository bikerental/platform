package com.bikerental.platform.rental.admin.controller;

import com.bikerental.platform.rental.admin.dto.CreateHotelRequest;
import com.bikerental.platform.rental.admin.dto.HotelResponse;
import com.bikerental.platform.rental.admin.dto.ResetPasswordRequest;
import com.bikerental.platform.rental.admin.service.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Admin endpoints for hotel management. Requires ROLE_ADMIN.
 * See docs/admin_api.md for usage examples.
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/hotels")
    public List<HotelResponse> listHotels() {
        return adminService.getAllHotels();
    }

    @PostMapping("/hotels")
    @ResponseStatus(HttpStatus.CREATED)
    public HotelResponse createHotel(@Valid @RequestBody CreateHotelRequest request) {
        return adminService.createHotel(request);
    }

    @PostMapping("/hotels/{hotelId}/reset-password")
    public Map<String, Object> resetPassword(
            @PathVariable Long hotelId,
            @Valid @RequestBody ResetPasswordRequest request) {
        adminService.resetPassword(hotelId, request.getNewPassword());
        return Map.of("message", "Password reset successfully", "hotelId", hotelId);
    }
}
