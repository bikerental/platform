package com.bikerental.platform.rental.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;

import com.bikerental.platform.rental.auth.security.JwtAuthenticationFilter;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@TestPropertySource(properties = {
        "jwt.secret=test-secret-key-that-is-at-least-256-bits-long-for-hmac-sha-256-algorithm",
        "jwt.expiration-hours=10",
        "admin.username=admin",
        "admin.password-hash=$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy"
})
class SecurityConfigTest {

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void passwordEncoder_IsConfigured() {
        // Assert
        assertThat(passwordEncoder).isNotNull();
    }

    @Test
    void passwordEncoder_EncodesPassword() {
        // Arrange
        String rawPassword = "testPassword123";

        // Act
        String encoded = passwordEncoder.encode(rawPassword);

        // Assert
        assertThat(encoded).isNotNull();
        assertThat(encoded).isNotEqualTo(rawPassword);
        assertThat(encoded).startsWith("$2a$"); // BCrypt prefix
    }

    @Test
    void passwordEncoder_MatchesPassword() {
        // Arrange
        String rawPassword = "testPassword123";
        String encoded = passwordEncoder.encode(rawPassword);

        // Act
        boolean matches = passwordEncoder.matches(rawPassword, encoded);

        // Assert
        assertThat(matches).isTrue();
    }

    @Test
    void passwordEncoder_DoesNotMatchWrongPassword() {
        // Arrange
        String rawPassword = "testPassword123";
        String wrongPassword = "wrongPassword";
        String encoded = passwordEncoder.encode(rawPassword);

        // Act
        boolean matches = passwordEncoder.matches(wrongPassword, encoded);

        // Assert
        assertThat(matches).isFalse();
    }
}
