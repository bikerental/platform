package com.bikerental.platform.rental.overview.controller;

import com.bikerental.platform.rental.overview.dto.OverviewResponse;
import com.bikerental.platform.rental.overview.service.OverviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for overview/dashboard data.
 */
@RestController
@RequestMapping("/api/overview")
@RequiredArgsConstructor
public class OverviewController {

    private final OverviewService overviewService;

    /**
     * Get overview data for the current hotel.
     * Returns bike counts, rental counts, and active/overdue rentals list.
     */
    @GetMapping
    public ResponseEntity<OverviewResponse> getOverview() {
        OverviewResponse overview = overviewService.getOverview();
        return ResponseEntity.ok(overview);
    }
}

