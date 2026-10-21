package io.ruleforge.common.tenant;

import java.util.UUID;

/**
 * Multi-tenant context for the current request, held in a {@link ScopedValue} (Java 25).
 * <p>
 * The security filters bind it for the duration of a request with {@link #runAs}; code
 * reads it with {@link #get()}. Unlike a ThreadLocal it cannot leak between requests that
 * reuse a thread, and the binding ends when the scope does. Work handed to other threads
 * (e.g. an executor) must re-bind it explicitly with {@link #runAs} or {@link #callAs}.
 */
public final class TenantContext {

    private static final ScopedValue<TenantInfo> CURRENT = ScopedValue.newInstance();

    private TenantContext() {}

    public record TenantInfo(UUID orgId, UUID spaceId, UUID userId, String role) {}

    /** Work that may throw a checked exception of type {@code X}. */
    @FunctionalInterface
    public interface TenantTask<X extends Throwable> {
        void run() throws X;
    }

    /** The tenant bound to the current scope, or {@code null} outside a tenant scope. */
    public static TenantInfo get() {
        // ScopedValue.orElse rejects a null fallback, so check the binding explicitly
        return CURRENT.isBound() ? CURRENT.get() : null;
    }

    /** Runs {@code task} with {@code info} as the current tenant. */
    public static <X extends Throwable> void runAs(TenantInfo info, TenantTask<X> task) throws X {
        ScopedValue.where(CURRENT, info).call(() -> {
            task.run();
            return null;
        });
    }

    /** Calls {@code op} with {@code info} as the current tenant and returns its result. */
    public static <T, X extends Throwable> T callAs(TenantInfo info, ScopedValue.CallableOp<T, X> op) throws X {
        return ScopedValue.where(CURRENT, info).call(op);
    }
}
