package com.bikerental.platform.rental.bike.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MarkOooRequest {
    @NotBlank(message = "Note is required")
    private String note;
}

