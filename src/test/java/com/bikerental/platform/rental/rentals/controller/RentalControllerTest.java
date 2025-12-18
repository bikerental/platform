package com.bikerental.platform.rental.rentals.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import com.bikerental.platform.rental.auth.security.HotelContext;
import com.bikerental.platform.rental.auth.service.JwtService;
import com.bikerental.platform.rental.common.exception.BikeUnavailableException;
import com.bikerental.platform.rental.config.SecurityConfig;
import com.bikerental.platform.rental.rentals.dto.CreateRentalRequest;
import com.bikerental.platform.rental.rentals.dto.RentalItemResponse;
import com.bikerental.platform.rental.rentals.dto.RentalResponse;
import com.bikerental.platform.rental.rentals.model.RentalItemStatus;
import com.bikerental.platform.rental.rentals.model.RentalStatus;
import com.bikerental.platform.rental.rentals.service.RentalContractService;
import com.bikerental.platform.rental.rentals.service.RentalService;

@WebMvcTest(RentalController.class)
@Import(SecurityConfig.class)
class RentalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RentalService rentalService;

    @MockBean
    private RentalContractService rentalContractService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private HotelContext hotelContext;

    private static final String ROOM_NUMBER = "204";
    private static final String BED_NUMBER = "A";
    private static final byte[] PNG_BYTES = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47};
    private static final String SIGNATURE_BASE64 = Base64.getEncoder().encodeToString(PNG_BYTES);

    private Instant futureReturnTime;
    private Instant startTime;

    @BeforeEach
    void setUp() {
        futureReturnTime = Instant.now().plus(24, ChronoUnit.HOURS);
        startTime = Instant.now();
    }

    @Test
    @WithMockUser(roles = "HOTEL")
    void createRental_WithValidRequest_Returns201() throws Exception {
        // Arrange
        RentalItemResponse item1 = new RentalItemResponse(1L, 10L, "B001", "ADULT", RentalItemStatus.RENTED);
        RentalItemResponse item2 = new RentalItemResponse(2L, 20L, "B002", "CHILD", RentalItemStatus.RENTED);
        
        RentalResponse response = new RentalResponse(
                1L, RentalStatus.ACTIVE, startTime, futureReturnTime,
                ROOM_NUMBER, BED_NUMBER, Arrays.asList(item1, item2)
        );
        
        when(rentalService.createRental(any(CreateRentalRequest.class))).thenReturn(response);

        String requestJson = """
            {
                "bikeNumbers": ["B001", "B002"],
                "roomNumber": "204",
                "bedNumber": "A",
                "returnDateTime": "%s",
                "tncVersion": "v1",
                "signatureBase64Png": "%s"
            }
            """.formatted(futureReturnTime.toString(), SIGNATURE_BASE64);

        // Act & Assert
        mockMvc.perform(post("/api/rentals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rentalId").value(1))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.roomNumber").value(ROOM_NUMBER))
                .andExpect(jsonPath("$.bedNumber").value(BED_NUMBER))
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.items[0].bikeNumber").value("B001"))
                .andExpect(jsonPath("$.items[0].status").value("RENTED"));
    }

    @Test
    @WithMockUser(roles = "HOTEL")
    void createRental_WithMissingBikeNumbers_Returns400() throws Exception {
        // Arrange
        String requestJson = """
            {
                "roomNumber": "204",
                "bedNumber": "A",
                "returnDateTime": "%s",
                "tncVersion": "v1",
                "signatureBase64Png": "%s"
            }
            """.formatted(futureReturnTime.toString(), SIGNATURE_BASE64);

        // Act & Assert
        mockMvc.perform(post("/api/rentals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "HOTEL")
    void createRental_WithEmptyBikeNumbers_Returns400() throws Exception {
        // Arrange
        String requestJson = """
            {
                "bikeNumbers": [],
                "roomNumber": "204",
                "bedNumber": "A",
                "returnDateTime": "%s",
                "tncVersion": "v1",
                "signatureBase64Png": "%s"
            }
            """.formatted(futureReturnTime.toString(), SIGNATURE_BASE64);

        // Act & Assert
        mockMvc.perform(post("/api/rentals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "HOTEL")
    void createRental_WithMissingRoomNumber_Returns400() throws Exception {
        // Arrange
        String requestJson = """
            {
                "bikeNumbers": ["B001"],
                "bedNumber": "A",
                "returnDateTime": "%s",
                "tncVersion": "v1",
                "signatureBase64Png": "%s"
            }
            """.formatted(futureReturnTime.toString(), SIGNATURE_BASE64);

        // Act & Assert
        mockMvc.perform(post("/api/rentals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "HOTEL")
    void createRental_WithMissingSignature_Returns400() throws Exception {
        // Arrange
        String requestJson = """
            {
                "bikeNumbers": ["B001"],
                "roomNumber": "204",
                "bedNumber": "A",
                "returnDateTime": "%s",
                "tncVersion": "v1"
            }
            """.formatted(futureReturnTime.toString());

        // Act & Assert
        mockMvc.perform(post("/api/rentals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "HOTEL")
    void createRental_WithUnavailableBikes_Returns409() throws Exception {
        // Arrange
        List<BikeUnavailableException.UnavailableBike> unavailable = Arrays.asList(
                new BikeUnavailableException.UnavailableBike("B001", "ALREADY_RENTED"),
                new BikeUnavailableException.UnavailableBike("B002", "OUT_OF_ORDER")
        );
        
        when(rentalService.createRental(any(CreateRentalRequest.class)))
                .thenThrow(new BikeUnavailableException("One or more bikes are unavailable", unavailable));

        String requestJson = """
            {
                "bikeNumbers": ["B001", "B002"],
                "roomNumber": "204",
                "bedNumber": "A",
                "returnDateTime": "%s",
                "tncVersion": "v1",
                "signatureBase64Png": "%s"
            }
            """.formatted(futureReturnTime.toString(), SIGNATURE_BASE64);

        // Act & Assert
        mockMvc.perform(post("/api/rentals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("BIKES_UNAVAILABLE"))
                .andExpect(jsonPath("$.details.unavailableBikes").isArray())
                .andExpect(jsonPath("$.details.unavailableBikes.length()").value(2))
                .andExpect(jsonPath("$.details.unavailableBikes[0].bikeNumber").value("B001"))
                .andExpect(jsonPath("$.details.unavailableBikes[0].reason").value("ALREADY_RENTED"));
    }

    @Test
    @WithMockUser(roles = "HOTEL")
    void createRental_WithNullBedNumber_Returns201() throws Exception {
        // Arrange
        RentalItemResponse item = new RentalItemResponse(1L, 10L, "B001", "ADULT", RentalItemStatus.RENTED);
        
        RentalResponse response = new RentalResponse(
                1L, RentalStatus.ACTIVE, startTime, futureReturnTime,
                ROOM_NUMBER, null, List.of(item)
        );
        
        when(rentalService.createRental(any(CreateRentalRequest.class))).thenReturn(response);

        String requestJson = """
            {
                "bikeNumbers": ["B001"],
                "roomNumber": "204",
                "returnDateTime": "%s",
                "tncVersion": "v1",
                "signatureBase64Png": "%s"
            }
            """.formatted(futureReturnTime.toString(), SIGNATURE_BASE64);

        // Act & Assert
        mockMvc.perform(post("/api/rentals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.bedNumber").doesNotExist());
    }

    @Test
    void createRental_WithoutAuthentication_DeniesAccess() throws Exception {
        // Arrange
        String requestJson = """
            {
                "bikeNumbers": ["B001"],
                "roomNumber": "204",
                "bedNumber": "A",
                "returnDateTime": "%s",
                "tncVersion": "v1",
                "signatureBase64Png": "%s"
            }
            """.formatted(futureReturnTime.toString(), SIGNATURE_BASE64);

        // Act & Assert
        // Spring Security returns 403 for anonymous users on protected endpoints
        mockMvc.perform(post("/api/rentals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isForbidden());
    }
}

