package com.bikerental.platform.rental.maintenance.controller;

import com.bikerental.platform.rental.auth.security.HotelContext;
import com.bikerental.platform.rental.bike.model.Bike;
import com.bikerental.platform.rental.bike.repo.BikeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Controller for maintenance-related operations.
 * Provides export functionality for OOO bikes.
 */
@RestController
@RequestMapping("/api/maintenance")
@RequiredArgsConstructor
public class MaintenanceController {

    private final BikeRepository bikeRepository;
    private final HotelContext hotelContext;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * Export OOO bikes as CSV for the current hotel.
     * Sorted by ooo_since ASC (oldest first), nulls last, then bike_number ASC.
     * 
     * Response headers:
     * - Content-Type: text/csv
     * - Content-Disposition: attachment; filename="ooo-bikes-YYYY-MM-DD.csv"
     */
    @GetMapping("/ooo/export")
    public ResponseEntity<String> exportOooBikes() {
        Long hotelId = hotelContext.getCurrentHotelId();
        List<Bike> oooBikes = bikeRepository.findOooBikesForExport(hotelId);

        String csv = generateCsv(oooBikes);
        String filename = "ooo-bikes-" + LocalDate.now().format(DATE_FORMATTER) + ".csv";

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(csv);
    }

    /**
     * Generate CSV content with proper escaping.
     * Always includes header row, even when no data rows.
     * Includes "sep=," hint for Excel to recognize comma delimiter.
     */
    private String generateCsv(List<Bike> bikes) {
        StringBuilder csv = new StringBuilder();
        
        // Excel hint: tells Excel to use comma as delimiter
        csv.append("sep=,\n");
        
        // Header row
        csv.append("bike_number,bike_type,ooo_note,ooo_since_date\n");

        // Data rows
        for (Bike bike : bikes) {
            csv.append(escapeCsvField(bike.getBikeNumber()));
            csv.append(",");
            csv.append(escapeCsvField(bike.getBikeType()));
            csv.append(",");
            csv.append(escapeCsvField(sanitizeForPrint(bike.getOooNote())));
            csv.append(",");
            csv.append(formatOooSince(bike));
            csv.append("\n");
        }

        return csv.toString();
    }

    /**
     * Sanitize text for print-friendly CSV output.
     * Replaces newlines with " | " separator so each bike stays on one row in Excel.
     */
    private String sanitizeForPrint(String value) {
        if (value == null) {
            return null;
        }
        // Replace all types of newlines with a print-friendly separator
        return value
                .replace("\r\n", " | ")
                .replace("\r", " | ")
                .replace("\n", " | ")
                .trim();
    }

    /**
     * Escape a CSV field value according to RFC 4180.
     * - If the field contains comma or quote, wrap in quotes
     * - Double any quote characters within the field
     * - Null values become empty strings
     */
    private String escapeCsvField(String value) {
        if (value == null) {
            return "";
        }
        
        // Check if escaping is needed (newlines already sanitized for print)
        boolean needsQuoting = value.contains(",") || value.contains("\"");
        
        if (needsQuoting) {
            // Escape quotes by doubling them and wrap in quotes
            String escaped = value.replace("\"", "\"\"");
            return "\"" + escaped + "\"";
        }
        
        return value;
    }

    /**
     * Format ooo_since as date string (YYYY-MM-DD) or empty if null.
     */
    private String formatOooSince(Bike bike) {
        if (bike.getOooSince() == null) {
            return "";
        }
        return bike.getOooSince()
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
                .format(DATE_FORMATTER);
    }
}

