package io.ruleforge.common.security;

import io.ruleforge.common.tenant.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * Continues a filter chain with the given tenant bound for the rest of the request.
 */
final class TenantScopedChain {

    private TenantScopedChain() {}

    static void proceed(TenantContext.TenantInfo tenant, FilterChain chain,
                        HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        try {
            TenantContext.runAs(tenant, () -> chain.doFilter(request, response));
        } catch (IOException | ServletException | RuntimeException | Error e) {
            throw e;
        } catch (Throwable t) {
            throw new ServletException(t);
        }
    }
}
