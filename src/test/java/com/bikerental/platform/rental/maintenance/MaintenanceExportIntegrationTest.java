package com.bikerental.platform.rental.maintenance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import com.bikerental.platform.rental.auth.model.Hotel;
import com.bikerental.platform.rental.auth.repo.HotelRepository;
import com.bikerental.platform.rental.auth.service.JwtService;
import com.bikerental.platform.rental.bike.model.Bike;
import com.bikerental.platform.rental.bike.repo.BikeRepository;

/**
 * Integration tests for OOO bikes Excel export.
 * Tests ordering, hotel scoping, Excel format, and edge cases.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@SuppressWarnings({"null", "unused"})
class MaintenanceExportIntegrationTest {

    private static final String EXCEL_CONTENT_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

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
        bikeRepository.deleteAll();
        hotelRepository.deleteAll();

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

        hotel1Token = jwtService.generateToken(hotel1.getHotelId(), hotel1.getHotelCode());
        hotel2Token = jwtService.generateToken(hotel2.getHotelId(), hotel2.getHotelCode());
    }

    @Test
    void exportOooBikes_WithOooBikes_ReturnsCorrectOrderingOldestFirst() throws Exception {
        Instant now = Instant.now();

        createOooBike(hotel1.getHotelId(), "B003", "ADULT", "Flat tire", now.minus(3, ChronoUnit.DAYS));
        createOooBike(hotel1.getHotelId(), "B001", "CHILD", "Broken chain", now.minus(1, ChronoUnit.DAYS));
        createOooBike(hotel1.getHotelId(), "B002", "ADULT", "Needs repair", now.minus(2, ChronoUnit.DAYS));

        MvcResult result = mockMvc.perform(get("/api/maintenance/ooo/export")
                        .header("Authorization", "Bearer " + hotel1Token))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", EXCEL_CONTENT_TYPE))
                .andExpect(header().exists("Content-Disposition"))
                .andReturn();

        List<String[]> rows = parseExcel(result.getResponse().getContentAsByteArray());

        assertThat(rows).hasSize(4); // header + 3 data rows
        assertThat(rows.get(0)[0]).isEqualTo("Bike Number"); // header
        assertThat(rows.get(1)[0]).isEqualTo("B003"); // oldest first
        assertThat(rows.get(2)[0]).isEqualTo("B002");
        assertThat(rows.get(3)[0]).isEqualTo("B001");
    }

    @Test
    void exportOooBikes_WithSameOooSince_OrdersByBikeNumber() throws Exception {
        Instant sameTime = Instant.now().minus(1, ChronoUnit.DAYS);

        createOooBike(hotel1.getHotelId(), "B003", "ADULT", "Note 3", sameTime);
        createOooBike(hotel1.getHotelId(), "B001", "ADULT", "Note 1", sameTime);
        createOooBike(hotel1.getHotelId(), "B002", "ADULT", "Note 2", sameTime);

        MvcResult result = mockMvc.perform(get("/api/maintenance/ooo/export")
                        .header("Authorization", "Bearer " + hotel1Token))
                .andExpect(status().isOk())
                .andReturn();

        List<String[]> rows = parseExcel(result.getResponse().getContentAsByteArray());

        assertThat(rows.get(1)[0]).isEqualTo("B001");
        assertThat(rows.get(2)[0]).isEqualTo("B002");
        assertThat(rows.get(3)[0]).isEqualTo("B003");
    }

    @Test
    void exportOooBikes_WithNullOooSince_PlacesNullsLast() throws Exception {
        Instant now = Instant.now();

        createOooBike(hotel1.getHotelId(), "B001", "ADULT", "Note", null);
        createOooBike(hotel1.getHotelId(), "B002", "ADULT", "Note 2", now.minus(1, ChronoUnit.DAYS));

        MvcResult result = mockMvc.perform(get("/api/maintenance/ooo/export")
                        .header("Authorization", "Bearer " + hotel1Token))
                .andExpect(status().isOk())
                .andReturn();

        List<String[]> rows = parseExcel(result.getResponse().getContentAsByteArray());

        assertThat(rows).hasSize(3); // header + 2 data rows
        assertThat(rows.get(1)[0]).isEqualTo("B002"); // has date - first
        assertThat(rows.get(2)[0]).isEqualTo("B001"); // null date - last
        assertThat(rows.get(2)[3]).isEmpty(); // empty OOO Since field
    }

    @Test
    void exportOooBikes_HotelScoping_ExcludesOtherHotelBikes() throws Exception {
        createOooBike(hotel1.getHotelId(), "H1-B001", "ADULT", "Hotel 1 bike",
                Instant.now().minus(1, ChronoUnit.DAYS));
        createOooBike(hotel2.getHotelId(), "H2-B001", "ADULT", "Hotel 2 bike",
                Instant.now().minus(1, ChronoUnit.DAYS));

        MvcResult result = mockMvc.perform(get("/api/maintenance/ooo/export")
                        .header("Authorization", "Bearer " + hotel1Token))
                .andExpect(status().isOk())
                .andReturn();

        List<String[]> rows = parseExcel(result.getResponse().getContentAsByteArray());

        assertThat(rows).hasSize(2); // header + 1 data row
        assertThat(rows.get(1)[0]).isEqualTo("H1-B001");
    }

    @Test
    void exportOooBikes_OnlyIncludesOooBikes_ExcludesAvailableAndRented() throws Exception {
        createOooBike(hotel1.getHotelId(), "OOO-BIKE", "ADULT", "Out of order",
                Instant.now().minus(1, ChronoUnit.DAYS));
        createBike(hotel1.getHotelId(), "AVAILABLE-BIKE", "ADULT", Bike.BikeStatus.AVAILABLE);
        createBike(hotel1.getHotelId(), "RENTED-BIKE", "CHILD", Bike.BikeStatus.RENTED);

        MvcResult result = mockMvc.perform(get("/api/maintenance/ooo/export")
                        .header("Authorization", "Bearer " + hotel1Token))
                .andExpect(status().isOk())
                .andReturn();

        List<String[]> rows = parseExcel(result.getResponse().getContentAsByteArray());

        assertThat(rows).hasSize(2); // header + 1 OOO bike
        assertThat(rows.get(1)[0]).isEqualTo("OOO-BIKE");
    }

    @Test
    void exportOooBikes_NoOooBikes_ReturnsHeaderOnly() throws Exception {
        createBike(hotel1.getHotelId(), "AVAILABLE-BIKE", "ADULT", Bike.BikeStatus.AVAILABLE);

        MvcResult result = mockMvc.perform(get("/api/maintenance/ooo/export")
                        .header("Authorization", "Bearer " + hotel1Token))
                .andExpect(status().isOk())
                .andReturn();

        List<String[]> rows = parseExcel(result.getResponse().getContentAsByteArray());

        assertThat(rows).hasSize(1); // header only
        assertThat(rows.get(0)[0]).isEqualTo("Bike Number");
    }

    @Test
    void exportOooBikes_SpecialCharacters_HandledCorrectly() throws Exception {
        Instant now = Instant.now();

        createOooBike(hotel1.getHotelId(), "B001", "ADULT",
                "Broken, needs \"repair\"", now.minus(2, ChronoUnit.DAYS));
        createOooBike(hotel1.getHotelId(), "B002", "CHILD",
                "Line1\nLine2\nLine3", now.minus(1, ChronoUnit.DAYS));

        MvcResult result = mockMvc.perform(get("/api/maintenance/ooo/export")
                        .header("Authorization", "Bearer " + hotel1Token))
                .andExpect(status().isOk())
                .andReturn();

        List<String[]> rows = parseExcel(result.getResponse().getContentAsByteArray());

        assertThat(rows).hasSize(3); // header + 2 data rows
        assertThat(rows.get(1)[2]).contains("Broken, needs \"repair\"");
        assertThat(rows.get(2)[2]).contains("Line1\nLine2\nLine3");
    }

    @Test
    void exportOooBikes_ExcelHeaders_CorrectContentTypeAndFilename() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/maintenance/ooo/export")
                        .header("Authorization", "Bearer " + hotel1Token))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", EXCEL_CONTENT_TYPE))
                .andReturn();

        String contentDisposition = result.getResponse().getHeader("Content-Disposition");

        assertThat(contentDisposition).startsWith("attachment; filename=\"ooo-bikes-");
        assertThat(contentDisposition).endsWith(".xlsx\"");
        assertThat(contentDisposition).matches("attachment; filename=\"ooo-bikes-\\d{4}-\\d{2}-\\d{2}\\.xlsx\"");
    }

    @Test
    void exportOooBikes_WithoutAuthentication_ReturnsForbidden() throws Exception {
        mockMvc.perform(get("/api/maintenance/ooo/export"))
                .andExpect(status().isForbidden());
    }

    @Test
    void exportOooBikes_NullBikeType_HandlesGracefully() throws Exception {
        Bike bike = new Bike();
        bike.setHotelId(hotel1.getHotelId());
        bike.setBikeNumber("B001");
        bike.setBikeType(null);
        bike.setStatus(Bike.BikeStatus.OOO);
        bike.setOooNote("Test note");
        bike.setOooSince(Instant.now());
        bikeRepository.save(bike);

        MvcResult result = mockMvc.perform(get("/api/maintenance/ooo/export")
                        .header("Authorization", "Bearer " + hotel1Token))
                .andExpect(status().isOk())
                .andReturn();

        List<String[]> rows = parseExcel(result.getResponse().getContentAsByteArray());

        assertThat(rows).hasSize(2);
        assertThat(rows.get(1)[0]).isEqualTo("B001");
        assertThat(rows.get(1)[1]).isEmpty(); // null bike type
    }

    // Helper methods

    private List<String[]> parseExcel(byte[] excelBytes) throws Exception {
        List<String[]> rows = new ArrayList<>();
        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(excelBytes))) {
            Sheet sheet = workbook.getSheetAt(0);
            for (Row row : sheet) {
                String[] cells = new String[4];
                for (int i = 0; i < 4; i++) {
                    cells[i] = row.getCell(i) != null ? row.getCell(i).getStringCellValue() : "";
                }
                rows.add(cells);
            }
        }
        return rows;
    }

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
