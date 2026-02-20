package io.ruleforge.common.tenant;

import java.util.UUID;

/**
 * ThreadLocal holder for multi-tenant context.
 * Set by the security filter from JWT claims or API key lookup.
 */
public final class TenantContext {

    private static final ThreadLocal<TenantInfo> CURRENT = new ThreadLocal<>();

    private TenantContext() {}

    public record TenantInfo(UUID orgId, UUID spaceId, UUID userId, String role) {}

    public static void set(TenantInfo info) {
        CURRENT.set(info);
    }

    public static TenantInfo get() {
        return CURRENT.get();
    }

    public static void clear() {
        CURRENT.remove();
    }
}
