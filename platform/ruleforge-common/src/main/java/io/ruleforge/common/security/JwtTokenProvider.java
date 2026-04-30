package io.ruleforge.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

/**
 * JWT token utility for generating and validating tokens.
 * Pure utility class with no Spring Security dependency.
 *
 * <p>Token claims layout:
 * <ul>
 *   <li>{@code sub} — userId (UUID string)</li>
 *   <li>{@code orgId} — organisation ID (UUID string)</li>
 *   <li>{@code email} — user email</li>
 *   <li>{@code role} — user role within the organisation</li>
 *   <li>{@code type} — "access" or "refresh"</li>
 * </ul>
 */
public class JwtTokenProvider {

    private static final String CLAIM_ORG_ID = "orgId";
    private static final String CLAIM_EMAIL = "email";
    private static final String CLAIM_ROLE = "role";
    private static final String CLAIM_TYPE = "type";

    private static final String TOKEN_TYPE_ACCESS = "access";
    private static final String TOKEN_TYPE_REFRESH = "refresh";

    private final SecretKey signingKey;
    private final long accessTokenValidityMs;
    private final long refreshTokenValidityMs;

    /**
     * @param base64Secret         Base64-encoded HMAC secret, or an explicit {@code local-dev-*} marker
     * @param accessTokenValidityMs  Access token TTL in milliseconds
     * @param refreshTokenValidityMs Refresh token TTL in milliseconds
     */
    public JwtTokenProvider(String base64Secret, long accessTokenValidityMs, long refreshTokenValidityMs) {
        byte[] keyBytes = decodeSigningKey(base64Secret);
        this.signingKey = new SecretKeySpec(keyBytes, "HmacSHA512");
        this.accessTokenValidityMs = accessTokenValidityMs;
        this.refreshTokenValidityMs = refreshTokenValidityMs;
    }

    private static byte[] decodeSigningKey(String secret) {
        if (secret != null && secret.startsWith("local-dev-")) {
            try {
                return MessageDigest.getInstance("SHA-512").digest(secret.getBytes(StandardCharsets.UTF_8));
            } catch (Exception e) {
                throw new IllegalStateException("Unable to derive local development JWT signing key", e);
            }
        }
        return Base64.getDecoder().decode(secret);
    }

    /**
     * Generate an access token containing user identity and role claims.
     */
    public String generateAccessToken(UUID userId, UUID orgId, String email, String role) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(userId.toString())
                .claim(CLAIM_ORG_ID, orgId.toString())
                .claim(CLAIM_EMAIL, email)
                .claim(CLAIM_ROLE, role)
                .claim(CLAIM_TYPE, TOKEN_TYPE_ACCESS)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(accessTokenValidityMs)))
                .signWith(signingKey)
                .compact();
    }

    /**
     * Generate a refresh token (minimal claims — only subject and type).
     */
    public String generateRefreshToken(UUID userId) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(userId.toString())
                .claim(CLAIM_TYPE, TOKEN_TYPE_REFRESH)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(refreshTokenValidityMs)))
                .signWith(signingKey)
                .compact();
    }

    /**
     * Parse and verify a token, returning the full set of claims.
     *
     * @throws JwtException if the token is invalid, expired, or tampered with
     */
    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Returns {@code true} if the token can be parsed and has not expired.
     */
    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public UUID getUserIdFromToken(String token) {
        return UUID.fromString(parseToken(token).getSubject());
    }

    public UUID getOrgIdFromToken(String token) {
        return UUID.fromString(parseToken(token).get(CLAIM_ORG_ID, String.class));
    }

    public String getRoleFromToken(String token) {
        return parseToken(token).get(CLAIM_ROLE, String.class);
    }

    public String getEmailFromToken(String token) {
        return parseToken(token).get(CLAIM_EMAIL, String.class);
    }

    public String getTokenType(String token) {
        return parseToken(token).get(CLAIM_TYPE, String.class);
    }
}
