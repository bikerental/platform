package com.bikerental.platform.rental.signature.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Base64;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.bikerental.platform.rental.signature.model.Signature;
import com.bikerental.platform.rental.signature.repo.SignatureRepository;

@ExtendWith(MockitoExtension.class)
class SignatureServiceTest {

    @Mock
    private SignatureRepository signatureRepository;

    @InjectMocks
    private SignatureService signatureService;

    private static final Long HOTEL_ID = 1L;
    private static final Long SIGNATURE_ID = 100L;
    private static final byte[] PNG_BYTES = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47}; // PNG magic bytes
    private static final String BASE64_PNG = Base64.getEncoder().encodeToString(PNG_BYTES);
    private static final String DATA_URL_PNG = "data:image/png;base64," + BASE64_PNG;

    @BeforeEach
    void setUp() {
    }

    @Test
    void storeSignature_WithValidBase64_SavesAndReturnsId() {
        // Arrange
        Signature savedSignature = new Signature(HOTEL_ID, PNG_BYTES);
        savedSignature.setSignatureId(SIGNATURE_ID);
        
        when(signatureRepository.save(any(Signature.class))).thenReturn(savedSignature);

        // Act
        Long result = signatureService.storeSignature(HOTEL_ID, BASE64_PNG);

        // Assert
        assertThat(result).isEqualTo(SIGNATURE_ID);
        
        ArgumentCaptor<Signature> captor = ArgumentCaptor.forClass(Signature.class);
        verify(signatureRepository).save(captor.capture());
        
        Signature captured = captor.getValue();
        assertThat(captured.getHotelId()).isEqualTo(HOTEL_ID);
        assertThat(captured.getSignatureData()).isEqualTo(PNG_BYTES);
    }

    @Test
    void storeSignature_WithDataUrlPrefix_StripsPrefix() {
        // Arrange
        Signature savedSignature = new Signature(HOTEL_ID, PNG_BYTES);
        savedSignature.setSignatureId(SIGNATURE_ID);
        
        when(signatureRepository.save(any(Signature.class))).thenReturn(savedSignature);

        // Act
        Long result = signatureService.storeSignature(HOTEL_ID, DATA_URL_PNG);

        // Assert
        assertThat(result).isEqualTo(SIGNATURE_ID);
        
        ArgumentCaptor<Signature> captor = ArgumentCaptor.forClass(Signature.class);
        verify(signatureRepository).save(captor.capture());
        
        Signature captured = captor.getValue();
        assertThat(captured.getSignatureData()).isEqualTo(PNG_BYTES);
    }

    @Test
    void storeSignature_WithNullBase64_ThrowsIllegalArgumentException() {
        // Act & Assert
        assertThatThrownBy(() -> signatureService.storeSignature(HOTEL_ID, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Signature data cannot be empty");
    }

    @Test
    void storeSignature_WithEmptyBase64_ThrowsIllegalArgumentException() {
        // Act & Assert
        assertThatThrownBy(() -> signatureService.storeSignature(HOTEL_ID, ""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Signature data cannot be empty");
    }

    @Test
    void storeSignature_WithBlankBase64_ThrowsIllegalArgumentException() {
        // Act & Assert
        assertThatThrownBy(() -> signatureService.storeSignature(HOTEL_ID, "   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Signature data cannot be empty");
    }

    @Test
    void storeSignature_WithInvalidBase64_ThrowsIllegalArgumentException() {
        // Act & Assert
        assertThatThrownBy(() -> signatureService.storeSignature(HOTEL_ID, "not-valid-base64!!!"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid base64 signature data");
    }

    @Test
    void getSignature_WhenExists_ReturnsSignature() {
        // Arrange
        Signature signature = new Signature(HOTEL_ID, PNG_BYTES);
        signature.setSignatureId(SIGNATURE_ID);
        
        when(signatureRepository.findBySignatureIdAndHotelId(SIGNATURE_ID, HOTEL_ID))
                .thenReturn(Optional.of(signature));

        // Act
        Optional<Signature> result = signatureService.getSignature(SIGNATURE_ID, HOTEL_ID);

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().getSignatureId()).isEqualTo(SIGNATURE_ID);
        assertThat(result.get().getSignatureData()).isEqualTo(PNG_BYTES);
    }

    @Test
    void getSignature_WhenNotExists_ReturnsEmpty() {
        // Arrange
        when(signatureRepository.findBySignatureIdAndHotelId(SIGNATURE_ID, HOTEL_ID))
                .thenReturn(Optional.empty());

        // Act
        Optional<Signature> result = signatureService.getSignature(SIGNATURE_ID, HOTEL_ID);

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    void getSignature_WhenDifferentHotel_ReturnsEmpty() {
        // Arrange
        Long otherHotelId = 999L;
        when(signatureRepository.findBySignatureIdAndHotelId(SIGNATURE_ID, otherHotelId))
                .thenReturn(Optional.empty());

        // Act
        Optional<Signature> result = signatureService.getSignature(SIGNATURE_ID, otherHotelId);

        // Assert
        assertThat(result).isEmpty();
    }
}

