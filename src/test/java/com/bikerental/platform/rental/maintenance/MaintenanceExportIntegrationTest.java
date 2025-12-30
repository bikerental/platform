package com.bikerental.platform.rental.maintenance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import com.bikerental.platform.rental.auth.model.Hotel;
import com.bikerental.platform.rental.auth.repo.HotelRepository;
import com.bikerental.platform.rental.auth.service.JwtService;
import com.bikerental.platform.rental.bike.model.Bike;
import com.bikerental.platform.rental.bike.repo.BikeRepository;

/**
 * Integration tests for OOO bikes CSV export.
 * Tests ordering, hotel scoping, CSV format, and edge cases.
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
@SuppressWarnings({"null", "unused"})
class MaintenanceExportIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private HotelRepository hotelRepository;

    @Autowired
    private BikeRepository bikeRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    private Hotel hotel1;
    private Hotel hotel2;
    private String hotel1Token;
    private String hotel2Token;

    @BeforeEach
    void setUp() {
        // Clear existing data
        bikeRepository.deleteAll();
        hotelRepository.deleteAll();

        // Create test hotels
        hotel1 = new Hotel();
        hotel1.setHotelCode("HOTEL1");
        hotel1.setHotelName("Test Hotel 1");
        hotel1.setPasswordHash(passwordEncoder.encode("password123"));
        hotel1 = hotelRepository.save(hotel1);

        hotel2 = new Hotel();
        hotel2.setHotelCode("HOTEL2");
        hotel2.setHotelName("Test Hotel 2");
        hotel2.setPasswordHash(passwordEncoder.encode("password123"));
        hotel2 = hotelRepository.save(hotel2);

        // Generate tokens
        hotel1Token = jwtService.generateToken(hotel1.getHotelId(), hotel1.getHotelCode());
        hotel2Token = jwtService.generateToken(hotel2.getHotelId(), hotel2.getHotelCode());
    }

    @Test
    void exportOooBikes_WithOooBikes_ReturnsCorrectOrderingOldestFirst() throws Exception {
        // Arrange: Create OOO bikes with different ooo_since dates
        Instant now = Instant.now();
        
        // Bike marked OOO 3 days ago (should be first)
        Bike oldestOoo = createOooBike(hotel1.getHotelId(), "B003", "ADULT", 
                "Flat tire", now.minus(3, ChronoUnit.DAYS));
        
        // Bike marked OOO 1 day ago (should be second)
        Bike middleOoo = createOooBike(hotel1.getHotelId(), "B001", "CHILD", 
                "Broken chain", now.minus(1, ChronoUnit.DAYS));
        
        // Bike marked OOO 2 days ago (should be between)
        Bike secondOldest = createOooBike(hotel1.getHotelId(), "B002", "ADULT", 
                "Needs repair", now.minus(2, ChronoUnit.DAYS));

        // Act
        MvcResult result = mockMvc.perform(get("/api/maintenance/ooo/export")
                        .header("Authorization", "Bearer " + hotel1Token))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "text/csv"))
                .andExpect(header().exists("Content-Disposition"))
                .andReturn();

        String csv = result.getResponse().getContentAsString();
        String[] lines = csv.split("\n");

        // Assert: sep hint + header + 3 data rows
        assertThat(lines).hasSize(5);
        assertThat(lines[0]).isEqualTo("sep=,");
        assertThat(lines[1]).isEqualTo("bike_number,bike_type,ooo_note,ooo_since_date");
        
        // Verify ordering: oldest OOO first (B003), then B002, then B001
        assertThat(lines[2]).startsWith("B003,ADULT,Flat tire,");
        assertThat(lines[3]).startsWith("B002,ADULT,Needs repair,");
        assertThat(lines[4]).startsWith("B001,CHILD,Broken chain,");
    }

    @Test
    void exportOooBikes_WithSameOooSince_OrdersByBikeNumber() throws Exception {
        // Arrange: Create OOO bikes with same ooo_since date
        Instant sameTime = Instant.now().minus(1, ChronoUnit.DAYS);
        
        createOooBike(hotel1.getHotelId(), "B003", "ADULT", "Note 3", sameTime);
        createOooBike(hotel1.getHotelId(), "B001", "ADULT", "Note 1", sameTime);
        createOooBike(hotel1.getHotelId(), "B002", "ADULT", "Note 2", sameTime);

        // Act
        MvcResult result = mockMvc.perform(get("/api/maintenance/ooo/export")
                        .header("Authorization", "Bearer " + hotel1Token))
                .andExpect(status().isOk())
                .andReturn();

        String csv = result.getResponse().getContentAsString();
        String[] lines = csv.split("\n");

        // Assert: Same ooo_since, so sort by bike_number ASC (lines[0]=sep, lines[1]=header)
        assertThat(lines[2]).startsWith("B001,");
        assertThat(lines[3]).startsWith("B002,");
        assertThat(lines[4]).startsWith("B003,");
    }

    @Test
    void exportOooBikes_WithNullOooSince_PlacesNullsLast() throws Exception {
        // Arrange
        Instant now = Instant.now();
        
        // Bike with null ooo_since (should be last)
        Bike nullOooBike = createOooBike(hotel1.getHotelId(), "B001", "ADULT", "Note", null);
        
        // Bike with ooo_since date (should be first)
        Bike datedOooBike = createOooBike(hotel1.getHotelId(), "B002", "ADULT", 
                "Note 2", now.minus(1, ChronoUnit.DAYS));

        // Act
        MvcResult result = mockMvc.perform(get("/api/maintenance/ooo/export")
                        .header("Authorization", "Bearer " + hotel1Token))
                .andExpect(status().isOk())
                .andReturn();

        String csv = result.getResponse().getContentAsString();
        String[] lines = csv.split("\n");

        // Assert: Bike with date comes first, null comes last (lines[0]=sep, lines[1]=header)
        assertThat(lines).hasSize(4);
        assertThat(lines[2]).startsWith("B002,"); // Has date - first
        assertThat(lines[3]).startsWith("B001,"); // Null date - last
        assertThat(lines[3]).endsWith(","); // Empty ooo_since_date field
    }

    @Test
    void exportOooBikes_HotelScoping_ExcludesOtherHotelBikes() throws Exception {
        // Arrange: Create OOO bikes for both hotels
        createOooBike(hotel1.getHotelId(), "H1-B001", "ADULT", "Hotel 1 bike", 
                Instant.now().minus(1, ChronoUnit.DAYS));
        createOooBike(hotel2.getHotelId(), "H2-B001", "ADULT", "Hotel 2 bike", 
                Instant.now().minus(1, ChronoUnit.DAYS));

        // Act: Export for hotel1
        MvcResult result = mockMvc.perform(get("/api/maintenance/ooo/export")
                        .header("Authorization", "Bearer " + hotel1Token))
                .andExpect(status().isOk())
                .andReturn();

        String csv = result.getResponse().getContentAsString();

        // Assert: Only hotel1's bike should appear
        assertThat(csv).contains("H1-B001");
        assertThat(csv).doesNotContain("H2-B001");
    }

    @Test
    void exportOooBikes_OnlyIncludesOooBikes_ExcludesAvailableAndRented() throws Exception {
        // Arrange: Create bikes with different statuses
        createOooBike(hotel1.getHotelId(), "OOO-BIKE", "ADULT", "Out of order", 
                Instant.now().minus(1, ChronoUnit.DAYS));
        createBike(hotel1.getHotelId(), "AVAILABLE-BIKE", "ADULT", Bike.BikeStatus.AVAILABLE);
        createBike(hotel1.getHotelId(), "RENTED-BIKE", "CHILD", Bike.BikeStatus.RENTED);

        // Act
        MvcResult result = mockMvc.perform(get("/api/maintenance/ooo/export")
                        .header("Authorization", "Bearer " + hotel1Token))
                .andExpect(status().isOk())
                .andReturn();

        String csv = result.getResponse().getContentAsString();

        // Assert: Only OOO bike included
        assertThat(csv).contains("OOO-BIKE");
        assertThat(csv).doesNotContain("AVAILABLE-BIKE");
        assertThat(csv).doesNotContain("RENTED-BIKE");
    }

    @Test
    void exportOooBikes_NoOooBikes_ReturnsHeaderOnly() throws Exception {
        // Arrange: Create only non-OOO bikes
        createBike(hotel1.getHotelId(), "AVAILABLE-BIKE", "ADULT", Bike.BikeStatus.AVAILABLE);

        // Act
        MvcResult result = mockMvc.perform(get("/api/maintenance/ooo/export")
                        .header("Authorization", "Bearer " + hotel1Token))
                .andExpect(status().isOk())
                .andReturn();

        String csv = result.getResponse().getContentAsString();

        // Assert: Only sep hint + header row (no data rows)
        assertThat(csv).isEqualTo("sep=,\nbike_number,bike_type,ooo_note,ooo_since_date\n");
    }

    @Test
    void exportOooBikes_CsvEscaping_HandlesCommasQuotesNewlines() throws Exception {
        // Arrange: Create OOO bike with special characters in note
        Instant now = Instant.now();
        
        createOooBike(hotel1.getHotelId(), "B001", "ADULT", 
                "Broken, needs \"repair\"", now.minus(2, ChronoUnit.DAYS));
        createOooBike(hotel1.getHotelId(), "B002", "CHILD", 
                "Line1\nLine2\nLine3", now.minus(1, ChronoUnit.DAYS));

        // Act
        MvcResult result = mockMvc.perform(get("/api/maintenance/ooo/export")
                        .header("Authorization", "Bearer " + hotel1Token))
                .andExpect(status().isOk())
                .andReturn();

        String csv = result.getResponse().getContentAsString();

        // Assert: Special characters are properly handled
        // Comma and quotes should be wrapped in quotes, quotes doubled
        assertThat(csv).contains("\"Broken, needs \"\"repair\"\"\"");
        // Newlines should be replaced with " | " for print-friendly output
        assertThat(csv).contains("Line1 | Line2 | Line3");
        // Verify no actual newlines in the note field (each bike on one row)
        String[] lines = csv.split("\n");
        assertThat(lines).hasSize(4); // sep + header + 2 data rows
    }

    @Test
    void exportOooBikes_CsvHeaders_CorrectContentTypeAndFilename() throws Exception {
        // Act
        MvcResult result = mockMvc.perform(get("/api/maintenance/ooo/export")
                        .header("Authorization", "Bearer " + hotel1Token))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "text/csv"))
                .andReturn();

        String contentDisposition = result.getResponse().getHeader("Content-Disposition");
        
        // Assert: Filename format is ooo-bikes-YYYY-MM-DD.csv
        assertThat(contentDisposition).startsWith("attachment; filename=\"ooo-bikes-");
        assertThat(contentDisposition).endsWith(".csv\"");
        assertThat(contentDisposition).matches("attachment; filename=\"ooo-bikes-\\d{4}-\\d{2}-\\d{2}\\.csv\"");
    }

    @Test
    void exportOooBikes_WithoutAuthentication_ReturnsForbidden() throws Exception {
        mockMvc.perform(get("/api/maintenance/ooo/export"))
                .andExpect(status().isForbidden());
    }

    @Test
    void exportOooBikes_NullBikeType_HandlesGracefully() throws Exception {
        // Arrange: Create OOO bike with null bike_type
        Bike bike = new Bike();
        bike.setHotelId(hotel1.getHotelId());
        bike.setBikeNumber("B001");
        bike.setBikeType(null);
        bike.setStatus(Bike.BikeStatus.OOO);
        bike.setOooNote("Test note");
        bike.setOooSince(Instant.now());
        bikeRepository.save(bike);

        // Act
        MvcResult result = mockMvc.perform(get("/api/maintenance/ooo/export")
                        .header("Authorization", "Bearer " + hotel1Token))
                .andExpect(status().isOk())
                .andReturn();

        String csv = result.getResponse().getContentAsString();
        String[] lines = csv.split("\n");

        // Assert: Null bike_type becomes empty field (lines[0]=sep, lines[1]=header)
        assertThat(lines[2]).startsWith("B001,,Test note,");
    }

    // Helper methods

    private Bike createOooBike(Long hotelId, String bikeNumber, String bikeType, 
                                String oooNote, Instant oooSince) {
        Bike bike = new Bike();
        bike.setHotelId(hotelId);
        bike.setBikeNumber(bikeNumber);
        bike.setBikeType(bikeType);
        bike.setStatus(Bike.BikeStatus.OOO);
        bike.setOooNote(oooNote);
        bike.setOooSince(oooSince);
        return bikeRepository.save(bike);
    }

    private Bike createBike(Long hotelId, String bikeNumber, String bikeType, 
                            Bike.BikeStatus status) {
        Bike bike = new Bike();
        bike.setHotelId(hotelId);
        bike.setBikeNumber(bikeNumber);
        bike.setBikeType(bikeType);
        bike.setStatus(status);
        return bikeRepository.save(bike);
    }
}

