package com.bikerental.platform.rental.auth.controller;

import com.bikerental.platform.rental.auth.dto.LoginRequest;
import com.bikerental.platform.rental.auth.dto.LoginResponse;
import com.bikerental.platform.rental.auth.service.AuthService;
import com.bikerental.platform.rental.common.dto.ErrorResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        return authService.authenticate(request)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity
                        .status(HttpStatus.UNAUTHORIZED)
                        .body(new ErrorResponse(
                                "INVALID_CREDENTIALS",
                                "Invalid hotel code or password",
                                null,
                                Instant.now().toString()
                        )));
    }
}
