package com.bikerental.platform.rental.signature.repo;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import com.bikerental.platform.rental.signature.model.Signature;

import java.nio.charset.StandardCharsets;

/**
 * Integration tests for SignatureRepository.
 */
@DataJpaTest
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.show-sql=true",
    "spring.sql.init.mode=never"
})
@Transactional
class SignatureRepositoryIntegrationTest {

    @Autowired
    private SignatureRepository signatureRepository;

    @BeforeEach
    void setUp() {
        signatureRepository.deleteAll();
    }

    @Test
    void saveSignature_WithValidData_Succeeds() {
        // Arrange
        byte[] signatureData = "fake-png-data".getBytes(StandardCharsets.UTF_8);
        Signature signature = new Signature(signatureData);

        // Act
        Signature saved = signatureRepository.save(signature);

        // Assert
        assertThat(saved.getSignatureId()).isNotNull();
        assertThat(saved.getSignatureData()).isEqualTo(signatureData);
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    void findById_AfterSave_ReturnsSignature() {
        // Arrange
        byte[] signatureData = "test-signature-png".getBytes(StandardCharsets.UTF_8);
        Signature signature = new Signature(signatureData);
        Signature saved = signatureRepository.save(signature);

        // Act
        var result = signatureRepository.findById(saved.getSignatureId());

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().getSignatureData()).isEqualTo(signatureData);
    }

    @Test
    void saveSignature_SetsCreatedAtAutomatically() {
        // Arrange
        Signature signature = new Signature("data".getBytes(StandardCharsets.UTF_8));
        assertThat(signature.getCreatedAt()).isNull();

        // Act
        Signature saved = signatureRepository.save(signature);

        // Assert
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    void saveMultipleSignatures_EachGetsDifferentId() {
        // Arrange
        Signature sig1 = new Signature("data1".getBytes(StandardCharsets.UTF_8));
        Signature sig2 = new Signature("data2".getBytes(StandardCharsets.UTF_8));

        // Act
        Signature saved1 = signatureRepository.save(sig1);
        Signature saved2 = signatureRepository.save(sig2);

        // Assert
        assertThat(saved1.getSignatureId()).isNotEqualTo(saved2.getSignatureId());
        assertThat(signatureRepository.count()).isEqualTo(2);
    }
}

