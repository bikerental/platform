package com.bikerental.platform.rental.maintenance.controller;

import com.bikerental.platform.rental.maintenance.service.MaintenanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Controller for maintenance-related operations.
 */
@RestController
@RequestMapping("/api/maintenance")
@RequiredArgsConstructor
public class MaintenanceController {

    private final MaintenanceService maintenanceService;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final String EXCEL_CONTENT_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    /**
     * Export OOO bikes as Excel (.xlsx) for the current hotel.
     */
    @GetMapping("/ooo/export")
    public ResponseEntity<byte[]> exportOooBikes() throws IOException {
        byte[] excelBytes = maintenanceService.exportOooBikesAsExcel();
        String filename = "ooo-bikes-" + LocalDate.now().format(DATE_FORMATTER) + ".xlsx";

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(EXCEL_CONTENT_TYPE))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(excelBytes);
    }
}
