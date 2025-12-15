package com.bikerental.platform.rental.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class HotelResponse {

    private Long hotelId;
    private String hotelCode;
    private String hotelName;
    private Instant createdAt;
}
