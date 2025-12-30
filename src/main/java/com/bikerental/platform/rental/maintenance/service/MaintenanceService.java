package com.bikerental.platform.rental.maintenance.service;

import com.bikerental.platform.rental.auth.security.HotelContext;
import com.bikerental.platform.rental.bike.model.Bike;
import com.bikerental.platform.rental.bike.repo.BikeRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Service for maintenance-related operations.
 * Handles OOO bikes export functionality.
 */
@Service
@RequiredArgsConstructor
public class MaintenanceService {

    private final BikeRepository bikeRepository;
    private final HotelContext hotelContext;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * Export OOO bikes as Excel bytes for the current hotel.
     * @return Excel file content as byte array
     */
    public byte[] exportOooBikesAsExcel() throws IOException {
        Long hotelId = hotelContext.getCurrentHotelId();
        List<Bike> oooBikes = bikeRepository.findOooBikesForExport(hotelId);
        return generateExcel(oooBikes);
    }

    /**
     * Generate Excel workbook with OOO bikes data.
     */
    private byte[] generateExcel(List<Bike> bikes) throws IOException {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("OOO Bikes");

            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            Row headerRow = sheet.createRow(0);
            String[] headers = {"Bike Number", "Bike Type", "OOO Note", "OOO Since"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowNum = 1;
            for (Bike bike : bikes) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(bike.getBikeNumber() != null ? bike.getBikeNumber() : "");
                row.createCell(1).setCellValue(bike.getBikeType() != null ? bike.getBikeType() : "");
                row.createCell(2).setCellValue(bike.getOooNote() != null ? bike.getOooNote() : "");
                row.createCell(3).setCellValue(formatOooSince(bike));
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        }
    }

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
