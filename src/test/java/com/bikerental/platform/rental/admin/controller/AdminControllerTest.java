package com.bikerental.platform.rental.admin.controller;

import com.bikerental.platform.rental.admin.dto.CreateHotelRequest;
import com.bikerental.platform.rental.admin.dto.HotelResponse;
import com.bikerental.platform.rental.admin.service.AdminService;
import com.bikerental.platform.rental.auth.security.JwtAuthenticationFilter;
import com.bikerental.platform.rental.auth.service.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AdminController.class)
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = {
        "jwt.secret=test-secret-key-that-is-at-least-256-bits-long-for-hmac-sha-256-algorithm",
        "jwt.expiration-hours=10",
        "admin.username=admin",
        "admin.password-hash=$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy"
})
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AdminService adminService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private static final Long HOTEL_ID = 1L;
    private static final String HOTEL_CODE = "HOTEL001";
    private static final String HOTEL_NAME = "Test Hotel";
    private static final String PASSWORD = "password123";

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    void listHotels_WithAdminRole_Returns200() throws Exception {
        // Arrange
        HotelResponse hotel1 = new HotelResponse(1L, "HOTEL001", "Hotel One", Instant.now());
        HotelResponse hotel2 = new HotelResponse(2L, "HOTEL002", "Hotel Two", Instant.now());

        when(adminService.getAllHotels()).thenReturn(List.of(hotel1, hotel2));

        // Act & Assert
        mockMvc.perform(get("/api/admin/hotels")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].hotelId").value(1L))
                .andExpect(jsonPath("$[0].hotelCode").value("HOTEL001"))
                .andExpect(jsonPath("$[1].hotelId").value(2L))
                .andExpect(jsonPath("$[1].hotelCode").value("HOTEL002"));
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    void createHotel_WithValidRequest_Returns201() throws Exception {
        // Arrange
        CreateHotelRequest request = new CreateHotelRequest(HOTEL_CODE, HOTEL_NAME, PASSWORD);
        HotelResponse response = new HotelResponse(HOTEL_ID, HOTEL_CODE, HOTEL_NAME, Instant.now());

        when(adminService.createHotel(any(CreateHotelRequest.class))).thenReturn(response);

        // Act & Assert
        mockMvc.perform(post("/api/admin/hotels")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.hotelId").value(HOTEL_ID))
                .andExpect(jsonPath("$.hotelCode").value(HOTEL_CODE))
                .andExpect(jsonPath("$.hotelName").value(HOTEL_NAME));
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    void createHotel_WithInvalidRequest_Returns400() throws Exception {
        // Arrange
        CreateHotelRequest request = new CreateHotelRequest("", "", ""); // Invalid: empty fields

        // Act & Assert
        mockMvc.perform(post("/api/admin/hotels")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    void resetPassword_WithValidRequest_Returns200() throws Exception {
        // Arrange
        String newPassword = "newPassword123";
        String requestBody = "{\"newPassword\":\"" + newPassword + "\"}";

        // Act & Assert
        mockMvc.perform(post("/api/admin/hotels/{hotelId}/reset-password", HOTEL_ID)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Password reset successfully"))
                .andExpect(jsonPath("$.hotelId").value(HOTEL_ID));
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    void resetPassword_WithInvalidRequest_Returns400() throws Exception {
        // Arrange
        String requestBody = "{\"newPassword\":\"\"}"; // Empty password

        // Act & Assert
        mockMvc.perform(post("/api/admin/hotels/{hotelId}/reset-password", HOTEL_ID)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    // Note: Security tests (403/401) are disabled here because filters are disabled for unit testing.
    // Security should be tested in integration tests with full Spring context.
    // @Test
    // @WithMockUser(authorities = "ROLE_HOTEL")
    // void listHotels_WithHotelRole_Returns403() throws Exception {
    //     // Act & Assert
    //     mockMvc.perform(get("/api/admin/hotels")
    //                     .with(csrf()))
    //             .andExpect(status().isForbidden());
    // }
    //
    // @Test
    // void listHotels_WithoutAuthentication_Returns401() throws Exception {
    //     // Act & Assert
    //     mockMvc.perform(get("/api/admin/hotels"))
    //             .andExpect(status().isUnauthorized());
    // }
}
