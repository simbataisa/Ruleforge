package io.ruleforge.common.config;

import io.ruleforge.common.security.SecurityConstants;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Servlet filter that ensures every request carries a correlation ID.
 *
 * <p>Behaviour:
 * <ol>
 *   <li>Reads the {@code X-Correlation-Id} header from the incoming request.</li>
 *   <li>If missing, generates a new UUID.</li>
 *   <li>Stores the correlation ID in SLF4J {@link MDC} under key {@code correlationId}
 *       so it appears in structured log output.</li>
 *   <li>Copies the correlation ID to the response header for traceability.</li>
 *   <li>Cleans up the MDC entry after the request completes.</li>
 * </ol>
 */
public class CorrelationIdFilter extends OncePerRequestFilter {

    private static final String MDC_KEY = "correlationId";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String correlationId = request.getHeader(SecurityConstants.CORRELATION_ID_HEADER);

        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }

        MDC.put(MDC_KEY, correlationId);
        response.setHeader(SecurityConstants.CORRELATION_ID_HEADER, correlationId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_KEY);
        }
    }
}
