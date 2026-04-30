package io.ruleforge.connector.service;

import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CredentialEncryptionServiceTest {

    private static final String VALID_TEST_KEY = Base64.getEncoder().encodeToString(new byte[32]);

    @Test
    void encryptCreatesVersionedGcmCiphertextAndDecryptsRoundTrip() {
        CredentialEncryptionService service = new CredentialEncryptionService(VALID_TEST_KEY);

        String ciphertext = service.encrypt("redis-password");

        assertTrue(ciphertext.startsWith("v2:"));
        assertNotEquals("redis-password", ciphertext);
        assertEquals("redis-password", service.decrypt(ciphertext));
    }

    @Test
    void decryptRejectsTamperedVersionedCiphertext() {
        CredentialEncryptionService service = new CredentialEncryptionService(VALID_TEST_KEY);
        String ciphertext = service.encrypt("tamper-sensitive");
        byte[] payload = Base64.getDecoder().decode(ciphertext.substring("v2:".length()));
        payload[payload.length - 1] ^= 0x01;
        String tampered = "v2:" + Base64.getEncoder().encodeToString(payload);

        assertThrows(RuntimeException.class, () -> service.decrypt(tampered));
    }
}
