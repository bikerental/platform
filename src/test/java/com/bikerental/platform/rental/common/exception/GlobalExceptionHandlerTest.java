package com.bikerental.platform.rental.common.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import com.bikerental.platform.rental.common.dto.ErrorResponse;

@SuppressWarnings("null")
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
    }

    @Test
    void handleValidationExceptions_ReturnsBadRequest() {
        // Arrange
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError = new FieldError("loginRequest", "hotelCode", "Hotel code is required");

        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getAllErrors()).thenReturn(Collections.singletonList(fieldError));

        // Act
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleValidationExceptions(ex);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        var body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getError()).isEqualTo("VALIDATION_ERROR");
        assertThat(body.getMessage()).isEqualTo("Validation failed");
        assertThat(body.getDetails()).isNotNull();
    }

    @Test
    void handleIllegalArgument_ReturnsBadRequest() {
        // Arrange
        IllegalArgumentException ex = new IllegalArgumentException("Invalid argument");

        // Act
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleIllegalArgument(ex);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        var body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getError()).isEqualTo("BAD_REQUEST");
        assertThat(body.getMessage()).isEqualTo("Invalid argument");
    }

    @Test
    void handleIllegalState_ReturnsConflict() {
        // Arrange
        IllegalStateException ex = new IllegalStateException("Resource conflict");

        // Act
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleIllegalState(ex);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        var body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getError()).isEqualTo("CONFLICT");
        assertThat(body.getMessage()).isEqualTo("Resource conflict");
    }

    @Test
    void handleNotFound_ReturnsNotFound() {
        // Arrange
        NotFoundException ex = new NotFoundException("Hotel not found");

        // Act
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleNotFound(ex);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        var body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getError()).isEqualTo("NOT_FOUND");
        assertThat(body.getMessage()).isEqualTo("Hotel not found");
    }

    @Test
    void handleConflict_ReturnsConflict() {
        // Arrange
        ConflictException ex = new ConflictException("Hotel code already exists");

        // Act
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleConflict(ex);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        var body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getError()).isEqualTo("CONFLICT");
        assertThat(body.getMessage()).isEqualTo("Hotel code already exists");
    }

    @Test
    void handleValidationExceptions_IncludesFieldErrors() {
        // Arrange
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError1 = new FieldError("request", "hotelCode", "Hotel code is required");
        FieldError fieldError2 = new FieldError("request", "password", "Password is required");

        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getAllErrors()).thenReturn(
                java.util.Arrays.asList(fieldError1, fieldError2));

        // Act
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleValidationExceptions(ex);

        // Assert
        var body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getDetails()).isNotNull();
        @SuppressWarnings("unchecked")
        Map<String, String> details = (Map<String, String>) body.getDetails();
        assertThat(details).containsKey("hotelCode");
        assertThat(details).containsKey("password");
    }
}
