package com.bikerental.platform.rental.rentals.service;

import com.bikerental.platform.rental.auth.security.HotelContext;
import com.bikerental.platform.rental.bike.model.Bike;
import com.bikerental.platform.rental.bike.repo.BikeRepository;
import com.bikerental.platform.rental.common.exception.NotFoundException;
import com.bikerental.platform.rental.rentals.model.Rental;
import com.bikerental.platform.rental.rentals.model.RentalItem;
import com.bikerental.platform.rental.rentals.repo.RentalRepository;
import com.bikerental.platform.rental.signature.model.Signature;
import com.bikerental.platform.rental.signature.service.SignatureService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

// Generates printable HTML contracts with embedded signatures
@Service
@RequiredArgsConstructor
public class RentalContractService {

    private final RentalRepository rentalRepository;
    private final BikeRepository bikeRepository;
    private final SignatureService signatureService;
    private final HotelContext hotelContext;

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("MMM d, yyyy 'at' h:mm a")
                    .withZone(ZoneId.systemDefault());

    @Transactional(readOnly = true)
    public byte[] getSignatureForRental(Long rentalId) {
        Long hotelId = hotelContext.getCurrentHotelId();

        Rental rental = rentalRepository.findByRentalIdAndHotelId(rentalId, hotelId)
                .orElseThrow(() -> new NotFoundException("Rental not found: " + rentalId));

        Signature signature = signatureService.getSignature(rental.getSignatureId(), hotelId)
                .orElseThrow(() -> new NotFoundException("Signature not found for rental: " + rentalId));

        return signature.getSignatureData();
    }

    @Transactional(readOnly = true)
    public String generateContractHtml(Long rentalId) {
        Long hotelId = hotelContext.getCurrentHotelId();

        Rental rental = rentalRepository.findByRentalIdAndHotelId(rentalId, hotelId)
                .orElseThrow(() -> new NotFoundException("Rental not found: " + rentalId));

        List<Long> bikeIds = rental.getItems().stream()
                .map(RentalItem::getBikeId)
                .collect(Collectors.toList());

        Map<Long, Bike> bikeMap = bikeRepository.findAllById(bikeIds).stream()
                .collect(Collectors.toMap(Bike::getBikeId, Function.identity()));

        String signatureBase64 = "";
        try {
            Signature signature = signatureService.getSignature(rental.getSignatureId(), hotelId)
                    .orElse(null);
            if (signature != null) {
                signatureBase64 = Base64.getEncoder().encodeToString(signature.getSignatureData());
            }
        } catch (Exception e) {
            // Signature unavailable
        }

        return buildContractHtml(rental, bikeMap, signatureBase64);
    }

    private String buildContractHtml(Rental rental, Map<Long, Bike> bikeMap, String signatureBase64) {
        StringBuilder html = new StringBuilder();
        
        html.append("<!DOCTYPE html>\n");
        html.append("<html lang=\"en\">\n");
        html.append("<head>\n");
        html.append("  <meta charset=\"UTF-8\">\n");
        html.append("  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
        html.append("  <title>Rental Contract #").append(rental.getRentalId()).append("</title>\n");
        html.append("  <style>\n");
        html.append("    * { box-sizing: border-box; margin: 0; padding: 0; }\n");
        html.append("    body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; ");
        html.append("line-height: 1.6; color: #1e293b; max-width: 800px; margin: 0 auto; padding: 40px 20px; }\n");
        html.append("    .header { text-align: center; margin-bottom: 40px; padding-bottom: 20px; border-bottom: 2px solid #e2e8f0; }\n");
        html.append("    .header h1 { font-size: 28px; font-weight: 700; color: #0f172a; margin-bottom: 8px; }\n");
        html.append("    .header .contract-id { font-size: 16px; color: #64748b; font-family: monospace; }\n");
        html.append("    .section { margin-bottom: 32px; }\n");
        html.append("    .section h2 { font-size: 18px; font-weight: 600; color: #334155; margin-bottom: 16px; ");
        html.append("padding-bottom: 8px; border-bottom: 1px solid #e2e8f0; }\n");
        html.append("    .info-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 16px; }\n");
        html.append("    .info-item { }\n");
        html.append("    .info-label { font-size: 12px; font-weight: 600; text-transform: uppercase; ");
        html.append("letter-spacing: 0.05em; color: #64748b; margin-bottom: 4px; }\n");
        html.append("    .info-value { font-size: 16px; color: #0f172a; }\n");
        html.append("    .bikes-table { width: 100%; border-collapse: collapse; }\n");
        html.append("    .bikes-table th, .bikes-table td { padding: 12px; text-align: left; border-bottom: 1px solid #e2e8f0; }\n");
        html.append("    .bikes-table th { font-size: 12px; font-weight: 600; text-transform: uppercase; ");
        html.append("letter-spacing: 0.05em; color: #64748b; background: #f8fafc; }\n");
        html.append("    .bikes-table td { font-size: 14px; }\n");
        html.append("    .status-badge { display: inline-block; padding: 4px 12px; border-radius: 9999px; ");
        html.append("font-size: 12px; font-weight: 600; text-transform: uppercase; }\n");
        html.append("    .status-rented { background: #dbeafe; color: #1d4ed8; }\n");
        html.append("    .status-returned { background: #dcfce7; color: #15803d; }\n");
        html.append("    .status-lost { background: #fee2e2; color: #dc2626; }\n");
        html.append("    .signature-section { margin-top: 40px; padding-top: 20px; border-top: 2px solid #e2e8f0; }\n");
        html.append("    .signature-img { max-width: 300px; max-height: 150px; border: 1px solid #e2e8f0; ");
        html.append("border-radius: 8px; background: #fff; }\n");
        html.append("    .tnc { margin-top: 40px; padding: 20px; background: #f8fafc; border-radius: 8px; ");
        html.append("font-size: 12px; color: #64748b; }\n");
        html.append("    .tnc h3 { font-size: 14px; font-weight: 600; color: #334155; margin-bottom: 8px; }\n");
        html.append("    @media print { body { padding: 20px; } }\n");
        html.append("  </style>\n");
        html.append("</head>\n");
        html.append("<body>\n");

        // Header
        html.append("  <div class=\"header\">\n");
        html.append("    <h1>Bike Rental Contract</h1>\n");
        html.append("    <div class=\"contract-id\">Contract #").append(rental.getRentalId()).append("</div>\n");
        html.append("  </div>\n");

        // Rental Details Section
        html.append("  <div class=\"section\">\n");
        html.append("    <h2>Rental Details</h2>\n");
        html.append("    <div class=\"info-grid\">\n");
        html.append("      <div class=\"info-item\">\n");
        html.append("        <div class=\"info-label\">Room Number</div>\n");
        html.append("        <div class=\"info-value\">").append(escapeHtml(rental.getRoomNumber())).append("</div>\n");
        html.append("      </div>\n");
        if (rental.getBedNumber() != null && !rental.getBedNumber().isEmpty()) {
            html.append("      <div class=\"info-item\">\n");
            html.append("        <div class=\"info-label\">Bed Number</div>\n");
            html.append("        <div class=\"info-value\">").append(escapeHtml(rental.getBedNumber())).append("</div>\n");
            html.append("      </div>\n");
        }
        html.append("      <div class=\"info-item\">\n");
        html.append("        <div class=\"info-label\">Start Date</div>\n");
        html.append("        <div class=\"info-value\">").append(DATE_FORMATTER.format(rental.getStartAt())).append("</div>\n");
        html.append("      </div>\n");
        html.append("      <div class=\"info-item\">\n");
        html.append("        <div class=\"info-label\">Due Date</div>\n");
        html.append("        <div class=\"info-value\">").append(DATE_FORMATTER.format(rental.getDueAt())).append("</div>\n");
        html.append("      </div>\n");
        html.append("      <div class=\"info-item\">\n");
        html.append("        <div class=\"info-label\">Status</div>\n");
        html.append("        <div class=\"info-value\">").append(rental.getStatus()).append("</div>\n");
        html.append("      </div>\n");
        html.append("    </div>\n");
        html.append("  </div>\n");

        // Bikes Section
        html.append("  <div class=\"section\">\n");
        html.append("    <h2>Rented Bikes</h2>\n");
        html.append("    <table class=\"bikes-table\">\n");
        html.append("      <thead>\n");
        html.append("        <tr>\n");
        html.append("          <th>Bike Number</th>\n");
        html.append("          <th>Type</th>\n");
        html.append("          <th>Status</th>\n");
        html.append("        </tr>\n");
        html.append("      </thead>\n");
        html.append("      <tbody>\n");
        
        for (RentalItem item : rental.getItems()) {
            Bike bike = bikeMap.get(item.getBikeId());
            String bikeNumber = bike != null ? bike.getBikeNumber() : "Unknown";
            String bikeType = bike != null && bike.getBikeType() != null ? bike.getBikeType() : "-";
            String statusClass = "status-" + item.getStatus().name().toLowerCase();
            
            html.append("        <tr>\n");
            html.append("          <td>").append(escapeHtml(bikeNumber)).append("</td>\n");
            html.append("          <td>").append(escapeHtml(bikeType)).append("</td>\n");
            html.append("          <td><span class=\"status-badge ").append(statusClass).append("\">");
            html.append(item.getStatus()).append("</span></td>\n");
            html.append("        </tr>\n");
        }
        
        html.append("      </tbody>\n");
        html.append("    </table>\n");
        html.append("  </div>\n");

        // Signature Section
        html.append("  <div class=\"section signature-section\">\n");
        html.append("    <h2>Guest Signature</h2>\n");
        if (!signatureBase64.isEmpty()) {
            html.append("    <img class=\"signature-img\" src=\"data:image/png;base64,");
            html.append(signatureBase64).append("\" alt=\"Guest Signature\" />\n");
        } else {
            html.append("    <p style=\"color: #64748b; font-style: italic;\">Signature not available</p>\n");
        }
        html.append("  </div>\n");

        html.append("  <div class=\"tnc\">\n");
        html.append("    <h3>Terms & Conditions (v").append(escapeHtml(rental.getTncVersion())).append(")</h3>\n");
        html.append("    <p>By signing this contract, the guest agrees to the following terms:</p>\n");
        html.append("    <ul style=\"margin-top: 8px; padding-left: 20px;\">\n");
        html.append("      <li>The guest is responsible for the rented bike(s) during the rental period.</li>\n");
        html.append("      <li>Bikes must be returned by the due date and time specified above.</li>\n");
        html.append("      <li>The guest is liable for any damage or loss of the bike(s).</li>\n");
        html.append("      <li>Late returns may incur additional charges.</li>\n");
        html.append("    </ul>\n");
        html.append("  </div>\n");

        html.append("</body>\n");
        html.append("</html>\n");

        return html.toString();
    }

    private String escapeHtml(String input) {
        if (input == null) return "";
        return input
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#x27;");
    }
}

