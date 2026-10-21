package io.ruleforge.common.tenant;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.InvocationInterceptor;
import org.junit.jupiter.api.extension.ReflectiveInvocationContext;

import java.lang.reflect.Method;

/**
 * Runs each test method with the tenant supplied by the test instance bound in {@link TenantContext}.
 * <p>
 * Usage: {@code @ExtendWith(TenantScopeExtension.class)} on a test class that implements
 * {@link TenantProvider}. Use {@link TenantContext#runAs} inside a test to switch tenant.
 */
public final class TenantScopeExtension implements InvocationInterceptor {

    /** Supplies the tenant to bind for each test method (may be {@code null} for no tenant). */
    public interface TenantProvider {
        TenantContext.TenantInfo tenant();
    }

    @Override
    public void interceptTestMethod(Invocation<Void> invocation,
                                    ReflectiveInvocationContext<Method> invocationContext,
                                    ExtensionContext extensionContext) throws Throwable {
        Object instance = extensionContext.getRequiredTestInstance();
        TenantContext.TenantInfo tenant = instance instanceof TenantProvider provider ? provider.tenant() : null;
        if (tenant == null) {
            invocation.proceed();
        } else {
            TenantContext.runAs(tenant, invocation::proceed);
        }
    }
}
