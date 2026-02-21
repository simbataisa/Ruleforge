package io.ruleforge.common.security;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Base64;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenProviderTest {

    private JwtTokenProvider provider;
    private static final long ACCESS_TTL = 3600_000L;
    private static final long REFRESH_TTL = 86400_000L;

    @BeforeEach
    void setUp() {
        // Generate a 512-bit key for HmacSHA512
        byte[] key = new byte[64];
        new java.security.SecureRandom().nextBytes(key);
        String base64Secret = Base64.getEncoder().encodeToString(key);
        provider = new JwtTokenProvider(base64Secret, ACCESS_TTL, REFRESH_TTL);
    }

    @Test
    void generateAccessToken_containsExpectedClaims() {
        UUID userId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();
        String email = "test@example.com";
        String role = "OWNER";

        String token = provider.generateAccessToken(userId, orgId, email, role);

        assertNotNull(token);
        assertTrue(provider.validateToken(token));
        assertEquals(userId, provider.getUserIdFromToken(token));
        assertEquals(orgId, provider.getOrgIdFromToken(token));
        assertEquals(email, provider.getEmailFromToken(token));
        assertEquals(role, provider.getRoleFromToken(token));
        assertEquals("access", provider.getTokenType(token));
    }

    @Test
    void generateRefreshToken_containsMinimalClaims() {
        UUID userId = UUID.randomUUID();

        String token = provider.generateRefreshToken(userId);

        assertNotNull(token);
        assertTrue(provider.validateToken(token));
        assertEquals(userId, provider.getUserIdFromToken(token));
        assertEquals("refresh", provider.getTokenType(token));
    }

    @Test
    void validateToken_returnsFalseForInvalidToken() {
        assertFalse(provider.validateToken("invalid.token.here"));
    }

    @Test
    void validateToken_returnsFalseForNull() {
        assertFalse(provider.validateToken(null));
    }

    @Test
    void parseToken_returnsClaimsForValidToken() {
        UUID userId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();
        String token = provider.generateAccessToken(userId, orgId, "user@test.com", "ADMIN");

        Claims claims = provider.parseToken(token);

        assertEquals(userId.toString(), claims.getSubject());
        assertEquals(orgId.toString(), claims.get("orgId", String.class));
    }

    @Test
    void differentProviderCannotValidateToken() {
        UUID userId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();
        String token = provider.generateAccessToken(userId, orgId, "user@test.com", "ADMIN");

        // Create a different provider with a different key
        byte[] otherKey = new byte[64];
        new java.security.SecureRandom().nextBytes(otherKey);
        String otherSecret = Base64.getEncoder().encodeToString(otherKey);
        JwtTokenProvider otherProvider = new JwtTokenProvider(otherSecret, ACCESS_TTL, REFRESH_TTL);

        assertFalse(otherProvider.validateToken(token));
    }
}
