package com.sigeclin.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class CryptoConverterTest {

    private CryptoConverter converter;

    @BeforeEach
    void setUp() {
        converter = new CryptoConverter();
        ReflectionTestUtils.setField(converter, "cryptoKey", "SigeclinSecureKeyDefault32BytesLong");
        converter.init();
    }

    @Test
    void testEncryptAndDecrypt() {
        String original = "Sensible clinical data 123";
        String encrypted = converter.convertToDatabaseColumn(original);
        assertNotNull(encrypted);
        assertNotEquals(original, encrypted);

        String decrypted = converter.convertToEntityAttribute(encrypted);
        assertEquals(original, decrypted);
    }

    @Test
    void testNullHandling() {
        assertNull(converter.convertToDatabaseColumn(null));
        assertNull(converter.convertToEntityAttribute(null));
    }

    @Test
    void testInvalidDecryptHandling() {
        String invalidData = "NotBase64EncodedData!!!";
        String decrypted = converter.convertToEntityAttribute(invalidData);
        assertEquals(invalidData, decrypted);
    }
}
