package io.ruleforge.event.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.ruleforge.event.exception.WebhookCredentialDecryptionException;
import io.ruleforge.event.model.WebhookAuthConfig;
import io.ruleforge.event.model.WebhookAuthType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class CredentialEncryptionServiceTest {

    // 32 random bytes Base64-encoded — valid AES-256 key for tests
    private static final String VALID_TEST_KEY =
            Base64.getEncoder().encodeToString(new byte[32]); // 32 zero bytes → valid 256-bit key

    private CredentialEncryptionService service;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        service = new CredentialEncryptionService(VALID_TEST_KEY, objectMapper);
    }

    // ------------------------------------------------------------------
    // Round-trip tests
    // ------------------------------------------------------------------

    @Test
    void encryptAndDecryptRoundTrip_signingSecret() {
        WebhookAuthConfig.SigningSecret original = new WebhookAuthConfig.SigningSecret("my-super-secret");

        String ciphertext = service.encrypt(original);
        WebhookAuthConfig decrypted = service.decrypt(ciphertext, WebhookAuthType.SIGNING_SECRET);

        assertInstanceOf(WebhookAuthConfig.SigningSecret.class, decrypted);
        WebhookAuthConfig.SigningSecret result = (WebhookAuthConfig.SigningSecret) decrypted;
        assertEquals("my-super-secret", result.secret());
    }

    @Test
    void encryptAndDecryptRoundTrip_apiKey() {
        WebhookAuthConfig.ApiKey original = new WebhookAuthConfig.ApiKey("X-Custom-Key", "api-key-value-123");

        String ciphertext = service.encrypt(original);
        WebhookAuthConfig decrypted = service.decrypt(ciphertext, WebhookAuthType.API_KEY);

        assertInstanceOf(WebhookAuthConfig.ApiKey.class, decrypted);
        WebhookAuthConfig.ApiKey result = (WebhookAuthConfig.ApiKey) decrypted;
        assertEquals("X-Custom-Key", result.headerName());
        assertEquals("api-key-value-123", result.apiKey());
    }

    @Test
    void encryptAndDecryptRoundTrip_basicAuth() {
        WebhookAuthConfig.BasicAuth original = new WebhookAuthConfig.BasicAuth("alice", "s3cr3tP@ss");

        String ciphertext = service.encrypt(original);
        WebhookAuthConfig decrypted = service.decrypt(ciphertext, WebhookAuthType.BASIC_AUTH);

        assertInstanceOf(WebhookAuthConfig.BasicAuth.class, decrypted);
        WebhookAuthConfig.BasicAuth result = (WebhookAuthConfig.BasicAuth) decrypted;
        assertEquals("alice", result.username());
        assertEquals("s3cr3tP@ss", result.password());
    }

    @Test
    void encryptAndDecryptRoundTrip_bearerToken() {
        WebhookAuthConfig.BearerToken original = new WebhookAuthConfig.BearerToken("eyJhbGciOiJIUzI1NiJ9.token");

        String ciphertext = service.encrypt(original);
        WebhookAuthConfig decrypted = service.decrypt(ciphertext, WebhookAuthType.BEARER_TOKEN);

        assertInstanceOf(WebhookAuthConfig.BearerToken.class, decrypted);
        WebhookAuthConfig.BearerToken result = (WebhookAuthConfig.BearerToken) decrypted;
        assertEquals("eyJhbGciOiJIUzI1NiJ9.token", result.token());
    }

    // ------------------------------------------------------------------
    // Random IV test — same plaintext must produce different ciphertexts
    // ------------------------------------------------------------------

    @Test
    void differentCiphertextEachCall() {
        WebhookAuthConfig.BearerToken config = new WebhookAuthConfig.BearerToken("static-token");

        String first = service.encrypt(config);
        String second = service.encrypt(config);

        assertNotEquals(first, second,
                "Each encrypt call must use a fresh random IV, producing a different ciphertext");
    }

    // ------------------------------------------------------------------
    // Tamper-detection test — GCM auth tag must catch flipped bytes
    // ------------------------------------------------------------------

    @Test
    void tamperDetectionThrowsException() {
        WebhookAuthConfig.SigningSecret config = new WebhookAuthConfig.SigningSecret("tamper-me");
        String ciphertext = service.encrypt(config);

        // Decode, flip a byte in the middle of the ciphertext portion (after the 12-byte IV)
        byte[] raw = Base64.getDecoder().decode(ciphertext);
        int flipIndex = raw.length / 2;
        raw[flipIndex] ^= 0xFF; // flip all bits in one byte
        String tampered = Base64.getEncoder().encodeToString(raw);

        assertThrows(WebhookCredentialDecryptionException.class,
                () -> service.decrypt(tampered, WebhookAuthType.SIGNING_SECRET),
                "Tampered ciphertext must throw WebhookCredentialDecryptionException");
    }

    // ------------------------------------------------------------------
    // Wrong key test
    // ------------------------------------------------------------------

    @Test
    void decryptWithWrongKeyThrowsException() {
        // Different 32-byte key
        byte[] differentKeyBytes = new byte[32];
        differentKeyBytes[0] = 0x01; // one byte different from VALID_TEST_KEY (all zeros)
        String differentKey = Base64.getEncoder().encodeToString(differentKeyBytes);
        CredentialEncryptionService otherService =
                new CredentialEncryptionService(differentKey, objectMapper);

        WebhookAuthConfig.BearerToken config = new WebhookAuthConfig.BearerToken("secret-token");
        String encryptedByOther = otherService.encrypt(config);

        // Attempt to decrypt with the original service (wrong key)
        assertThrows(WebhookCredentialDecryptionException.class,
                () -> service.decrypt(encryptedByOther, WebhookAuthType.BEARER_TOKEN),
                "Decryption with wrong key must throw WebhookCredentialDecryptionException");
    }

    // ------------------------------------------------------------------
    // Wrong key length test — constructor must reject short keys
    // ------------------------------------------------------------------

    @Test
    void wrongKeyLengthThrowsException() {
        // "short" is 5 bytes when Base64-decoded from a raw-bytes perspective,
        // but Base64.encode("tooshort".getBytes()) decodes to 7 bytes — we want < 32 bytes
        String shortKey = Base64.getEncoder().encodeToString("tooshort-key".getBytes()); // 12 bytes decoded

        assertThrows(IllegalArgumentException.class,
                () -> new CredentialEncryptionService(shortKey, objectMapper),
                "Constructor must reject keys that do not decode to exactly 32 bytes");
    }
}
