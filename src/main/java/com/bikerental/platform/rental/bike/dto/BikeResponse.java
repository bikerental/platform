package com.bikerental.platform.rental.bike.dto;

import com.bikerental.platform.rental.bike.model.Bike;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BikeResponse {
    private Long bikeId;
    private String bikeNumber;
    private String bikeType;
    private Bike.BikeStatus status;
    private String oooNote;
    private Instant oooSince;
}

