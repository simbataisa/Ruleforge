package io.ruleforge.connector.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.AEADBadTagException;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES-256 encryption service for connector credentials.
 */
@Service
public class CredentialEncryptionService {

    private static final String CIPHERTEXT_VERSION = "v2:";
    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH_BYTES = 12;
    private static final int GCM_TAG_LENGTH_BITS = 128;

    private final SecretKeySpec secretKey;
    private final SecureRandom secureRandom = new SecureRandom();

    public CredentialEncryptionService(
            @Value("${ruleforge.encryption.secret-key:local-dev-connector-credential-key}") String base64Key) {
        byte[] keyBytes = decodeKey(base64Key);
        if (keyBytes.length != 32) {
            throw new IllegalArgumentException(
                    "Connector credential encryption key must decode to exactly 32 bytes");
        }
        this.secretKey = new SecretKeySpec(keyBytes, "AES");
    }

    private static byte[] decodeKey(String key) {
        if (key != null && key.startsWith("local-dev-")) {
            try {
                return MessageDigest.getInstance("SHA-256").digest(key.getBytes(StandardCharsets.UTF_8));
            } catch (Exception e) {
                throw new IllegalStateException("Unable to derive local development credential key", e);
            }
        }
        return Base64.getDecoder().decode(key);
    }

    public String encrypt(String plaintext) {
        if (plaintext == null) return null;
        try {
            byte[] iv = new byte[GCM_IV_LENGTH_BYTES];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            byte[] combined = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);

            return CIPHERTEXT_VERSION + Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new RuntimeException("Encryption failed", e);
        }
    }

    public String decrypt(String ciphertext) {
        if (ciphertext == null) return null;
        if (!ciphertext.startsWith(CIPHERTEXT_VERSION)) {
            throw new RuntimeException("Unsupported connector credential ciphertext version");
        }

        try {
            byte[] combined = Base64.getDecoder().decode(ciphertext.substring(CIPHERTEXT_VERSION.length()));
            if (combined.length <= GCM_IV_LENGTH_BYTES) {
                throw new RuntimeException("Connector credential ciphertext is too short");
            }

            byte[] iv = new byte[GCM_IV_LENGTH_BYTES];
            byte[] encrypted = new byte[combined.length - GCM_IV_LENGTH_BYTES];
            System.arraycopy(combined, 0, iv, 0, iv.length);
            System.arraycopy(combined, iv.length, encrypted, 0, encrypted.length);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (AEADBadTagException e) {
            throw new RuntimeException("Decryption failed: ciphertext was tampered with or key is incorrect", e);
        } catch (Exception e) {
            throw new RuntimeException("Decryption failed", e);
        }
    }
}
