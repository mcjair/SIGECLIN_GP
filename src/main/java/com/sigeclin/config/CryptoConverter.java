package com.sigeclin.config;

import jakarta.annotation.PostConstruct;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

@Component
@Converter
public class CryptoConverter implements AttributeConverter<String, String> {

    private static final String ALGORITHM_MODE = "AES/GCM/NoPadding";
    private static final String ALGORITHM_PURE = "AES";

    private static SecretKeySpec staticKeySpec;

    @Value("${app.security.crypto-key}")
    private String cryptoKey;

    private SecretKeySpec keySpec;
    private final SecureRandom secureRandom = new SecureRandom();

    @PostConstruct
    public void init() {
        byte[] keyBytes = cryptoKey.getBytes(StandardCharsets.UTF_8);
        byte[] finalKeyBytes = new byte[32];
        System.arraycopy(keyBytes, 0, finalKeyBytes, 0, Math.min(keyBytes.length, 32));
        staticKeySpec = new SecretKeySpec(finalKeyBytes, ALGORITHM_PURE);
        this.keySpec = staticKeySpec;
    }

    private SecretKeySpec getEffectiveKeySpec() {
        if (this.keySpec != null) {
            return this.keySpec;
        }
        if (staticKeySpec != null) {
            return staticKeySpec;
        }
        // Fallback robusto
        byte[] keyBytes = "SigeclinSecureKeyDefault32BytesLong".getBytes(StandardCharsets.UTF_8);
        byte[] finalKeyBytes = new byte[32];
        System.arraycopy(keyBytes, 0, finalKeyBytes, 0, Math.min(keyBytes.length, 32));
        staticKeySpec = new SecretKeySpec(finalKeyBytes, ALGORITHM_PURE);
        return staticKeySpec;
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null) {
            return null;
        }
        try {
            byte[] iv = new byte[12];
            secureRandom.nextBytes(iv);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(128, iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM_MODE);
            cipher.init(Cipher.ENCRYPT_MODE, getEffectiveKeySpec(), parameterSpec);
            byte[] encryptedData = cipher.doFinal(attribute.getBytes(StandardCharsets.UTF_8));

            byte[] combined = new byte[iv.length + encryptedData.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(encryptedData, 0, combined, iv.length, encryptedData.length);

            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new RuntimeException("Error al cifrar el dato clínico", e);
        }
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        try {
            byte[] combined = Base64.getDecoder().decode(dbData);
            if (combined.length < 12) {
                return dbData;
            }

            byte[] iv = new byte[12];
            System.arraycopy(combined, 0, iv, 0, 12);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(128, iv);

            int encryptedSize = combined.length - 12;
            byte[] encryptedData = new byte[encryptedSize];
            System.arraycopy(combined, 12, encryptedData, 0, encryptedSize);

            Cipher cipher = Cipher.getInstance(ALGORITHM_MODE);
            cipher.init(Cipher.DECRYPT_MODE, getEffectiveKeySpec(), parameterSpec);
            return new String(cipher.doFinal(encryptedData), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return dbData;
        }
    }
}
