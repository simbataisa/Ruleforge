package io.ruleforge.common.security;

/**
 * Shared security constants used across all RuleForge services.
 */
public final class SecurityConstants {

    private SecurityConstants() {}

    // Header constants
    public static final String TOKEN_PREFIX = "Bearer ";
    public static final String HEADER_STRING = "Authorization";
    public static final String API_KEY_HEADER = "X-API-Key";
    public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    public static final String TENANT_HEADER = "X-Tenant-Id";

    // Role constants
    public static final String ROLE_OWNER = "OWNER";
    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_EDITOR = "EDITOR";
    public static final String ROLE_VIEWER = "VIEWER";
    public static final String ROLE_SOLVER = "SOLVER";
}
