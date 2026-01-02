package com.bikerental.platform.rental.auth.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import com.bikerental.platform.rental.auth.dto.LoginRequest;
import com.bikerental.platform.rental.auth.dto.LoginResponse;
import com.bikerental.platform.rental.auth.security.JwtAuthenticationFilter;
import com.bikerental.platform.rental.auth.service.AuthService;
import com.bikerental.platform.rental.auth.service.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = {
        "jwt.secret=test-secret-key-that-is-at-least-256-bits-long-for-hmac-sha-256-algorithm",
        "jwt.expiration-hours=10"
})
@SuppressWarnings("null")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private static final String HOTEL_CODE = "HOTEL001";
    private static final String PASSWORD = "password123";
    private static final String JWT_TOKEN = "test.jwt.token";
    private static final String HOTEL_NAME = "Test Hotel";

    @Test
    void login_WithValidCredentials_Returns200AndToken() throws Exception {
        // Arrange
        LoginRequest request = new LoginRequest(HOTEL_CODE, PASSWORD);
        LoginResponse response = new LoginResponse(JWT_TOKEN, HOTEL_NAME);

        when(authService.authenticate(any(LoginRequest.class))).thenReturn(Optional.of(response));

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.accessToken").value(JWT_TOKEN))
                .andExpect(jsonPath("$.hotelName").value(HOTEL_NAME));
    }

    @Test
    void login_WithInvalidCredentials_Returns401() throws Exception {
        // Arrange
        LoginRequest request = new LoginRequest(HOTEL_CODE, "wrongPassword");

        when(authService.authenticate(any(LoginRequest.class))).thenReturn(Optional.empty());

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.message").value("Invalid hotel code or password"));
    }

    @Test
    void login_WithMissingHotelCode_Returns400() throws Exception {
        // Arrange
        LoginRequest request = new LoginRequest("", PASSWORD);

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    void login_WithMissingPassword_Returns400() throws Exception {
        // Arrange
        LoginRequest request = new LoginRequest(HOTEL_CODE, "");

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    void login_WithNullBody_Returns400() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }
}
