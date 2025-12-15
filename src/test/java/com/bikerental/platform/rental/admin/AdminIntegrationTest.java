package com.bikerental.platform.rental.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.bikerental.platform.rental.auth.model.Hotel;
import com.bikerental.platform.rental.auth.repo.HotelRepository;
import com.bikerental.platform.rental.auth.service.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Integration tests for Admin API endpoints.
 * Tests the full flow including authentication, database operations, and security.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestPropertySource(properties = {
        "jwt.secret=test-secret-key-that-is-at-least-256-bits-long-for-hmac-sha-256-algorithm",
        "jwt.expiration-hours=10",
        "admin.username=admin",
        "admin.password-hash=$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy"
})
@SuppressWarnings("null")
class AdminIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private HotelRepository hotelRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    private String adminToken;

    @BeforeEach
    void setUp() {
        // Clear existing hotels
        hotelRepository.deleteAll();
        
        // Generate admin token
        adminToken = jwtService.generateAdminToken("admin");
    }

    @Test
    void testCompleteAdminWorkflow() throws Exception {
        // Step 1: List hotels (should be empty initially)
        mockMvc.perform(get("/api/admin/hotels")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));

        // Step 2: Create a new hotel
        String createHotelRequest = """
                {
                    "hotelCode": "TESTHOTEL",
                    "hotelName": "Test Hotel",
                    "password": "TestPassword123"
                }
                """;

        String response = mockMvc.perform(post("/api/admin/hotels")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createHotelRequest))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.hotelId").exists())
                .andExpect(jsonPath("$.hotelCode").value("TESTHOTEL"))
                .andExpect(jsonPath("$.hotelName").value("Test Hotel"))
                .andExpect(jsonPath("$.createdAt").exists())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Extract hotel ID from response
        Long hotelId = objectMapper.readTree(response).get("hotelId").asLong();

        // Step 3: Verify hotel was created in database
        Hotel createdHotel = hotelRepository.findByHotelCode("TESTHOTEL").orElseThrow();
        assertThat(createdHotel.getHotelCode()).isEqualTo("TESTHOTEL");
        assertThat(createdHotel.getHotelName()).isEqualTo("Test Hotel");
        assertThat(createdHotel.getPasswordHash()).isNotNull();
        assertThat(passwordEncoder.matches("TestPassword123", createdHotel.getPasswordHash())).isTrue();

        // Step 4: List hotels again (should now have 1 hotel)
        mockMvc.perform(get("/api/admin/hotels")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].hotelCode").value("TESTHOTEL"));

        // Step 5: Reset the hotel's password
        String resetPasswordRequest = """
                {
                    "newPassword": "NewPassword456"
                }
                """;

        mockMvc.perform(post("/api/admin/hotels/{hotelId}/reset-password", hotelId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(resetPasswordRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Password reset successfully"))
                .andExpect(jsonPath("$.hotelId").value(hotelId));

        // Step 6: Verify password was actually changed in database
        Hotel updatedHotel = hotelRepository.findById(hotelId).orElseThrow();
        assertThat(passwordEncoder.matches("NewPassword456", updatedHotel.getPasswordHash())).isTrue();
        assertThat(passwordEncoder.matches("TestPassword123", updatedHotel.getPasswordHash())).isFalse();
    }

    @Test
    void testCreateHotel_WithDuplicateCode_Returns409() throws Exception {
        // Create first hotel
        Hotel existingHotel = new Hotel();
        existingHotel.setHotelCode("EXISTING");
        existingHotel.setHotelName("Existing Hotel");
        existingHotel.setPasswordHash(passwordEncoder.encode("password123"));
        hotelRepository.save(existingHotel);

        // Try to create another hotel with same code
        String createHotelRequest = """
                {
                    "hotelCode": "EXISTING",
                    "hotelName": "Another Hotel",
                    "password": "Password123"
                }
                """;

        mockMvc.perform(post("/api/admin/hotels")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createHotelRequest))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("CONFLICT"))
                .andExpect(jsonPath("$.message").value("Hotel code 'EXISTING' already exists"));
    }

    @Test
    void testCreateHotel_WithInvalidData_Returns400() throws Exception {
        // Test with empty hotel code
        String invalidRequest1 = """
                {
                    "hotelCode": "",
                    "hotelName": "Test Hotel",
                    "password": "Password123"
                }
                """;

        mockMvc.perform(post("/api/admin/hotels")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRequest1))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));

        // Test with password too short
        String invalidRequest2 = """
                {
                    "hotelCode": "TEST",
                    "hotelName": "Test Hotel",
                    "password": "short"
                }
                """;

        mockMvc.perform(post("/api/admin/hotels")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRequest2))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    void testResetPassword_WithInvalidHotelId_Returns404() throws Exception {
        String resetPasswordRequest = """
                {
                    "newPassword": "NewPassword456"
                }
                """;

        mockMvc.perform(post("/api/admin/hotels/999/reset-password")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(resetPasswordRequest))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Hotel not found with ID: 999"));
    }

    @Test
    void testResetPassword_WithInvalidPassword_Returns400() throws Exception {
        // Create a hotel first
        Hotel hotel = new Hotel();
        hotel.setHotelCode("TEST");
        hotel.setHotelName("Test Hotel");
        hotel.setPasswordHash(passwordEncoder.encode("password123"));
        Hotel savedHotel = hotelRepository.save(hotel);

        // Try to reset with password too short
        String invalidRequest = """
                {
                    "newPassword": "short"
                }
                """;

        mockMvc.perform(post("/api/admin/hotels/{hotelId}/reset-password", savedHotel.getHotelId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRequest))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    void testAdminEndpoints_WithoutAuthentication_ReturnsForbidden() throws Exception {
        // Spring Security returns 403 Forbidden for unauthenticated requests to protected endpoints
        // (unless a custom AuthenticationEntryPoint is configured to return 401)
        mockMvc.perform(get("/api/admin/hotels"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/admin/hotels")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void testAdminEndpoints_WithHotelToken_Returns403() throws Exception {
        // Create a hotel and get its token (not admin token)
        Hotel hotel = new Hotel();
        hotel.setHotelCode("HOTEL");
        hotel.setHotelName("Hotel");
        hotel.setPasswordHash(passwordEncoder.encode("password123"));
        Hotel savedHotel = hotelRepository.save(hotel);

        String hotelToken = jwtService.generateToken(savedHotel.getHotelId(), savedHotel.getHotelCode());

        // Try to access admin endpoints with hotel token
        mockMvc.perform(get("/api/admin/hotels")
                        .header("Authorization", "Bearer " + hotelToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/admin/hotels")
                        .header("Authorization", "Bearer " + hotelToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }
}
