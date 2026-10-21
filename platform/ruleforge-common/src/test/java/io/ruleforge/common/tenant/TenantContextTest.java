package io.ruleforge.common.tenant;

import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class TenantContextTest {

    private final TenantContext.TenantInfo acme =
            new TenantContext.TenantInfo(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "OWNER");
    private final TenantContext.TenantInfo other =
            new TenantContext.TenantInfo(UUID.randomUUID(), null, UUID.randomUUID(), "VIEWER");

    @Test
    void getReturnsNullOutsideAScope() {
        assertNull(TenantContext.get());
    }

    @Test
    void runAsBindsTheTenantOnlyForTheScope() {
        AtomicReference<TenantContext.TenantInfo> seen = new AtomicReference<>();

        TenantContext.runAs(acme, () -> seen.set(TenantContext.get()));

        assertEquals(acme, seen.get());
        assertNull(TenantContext.get());
    }

    @Test
    void nestedScopeOverridesThenRestoresTheOuterTenant() {
        TenantContext.runAs(acme, () -> {
            TenantContext.runAs(other, () -> assertEquals(other, TenantContext.get()));
            assertEquals(acme, TenantContext.get());
        });
    }

    @Test
    void bindingEndsEvenWhenTheScopeThrows() {
        assertThrows(IllegalStateException.class, () -> TenantContext.runAs(acme, () -> {
            throw new IllegalStateException("boom");
        }));
        assertNull(TenantContext.get());
    }

    @Test
    void callAsReturnsTheResult() {
        UUID orgId = TenantContext.callAs(acme, () -> TenantContext.get().orgId());
        assertEquals(acme.orgId(), orgId);
    }

    @Test
    void bindingIsNotVisibleFromAnotherThread() throws Exception {
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            TenantContext.runAs(acme, () -> {
                TenantContext.TenantInfo onOtherThread = executor.submit(TenantContext::get).get();
                assertNull(onOtherThread);
            });
        }
    }
}
