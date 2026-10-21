package io.ruleforge.common.security;

import io.ruleforge.common.tenant.TenantContext;
import io.ruleforge.common.tenant.TenantContext.TenantInfo;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

/**
 * Spring Web filter that authenticates requests using the {@code X-API-Key} header.
 * Delegates key validation to an {@link ApiKeyValidator} implementation.
 *
 * <p>This filter only activates when the API key header is present and no
 * authentication has been set yet (i.e., the JWT filter did not match).
 */
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(ApiKeyAuthenticationFilter.class);

    private final ApiKeyValidator apiKeyValidator;

    public ApiKeyAuthenticationFilter(ApiKeyValidator apiKeyValidator) {
        this.apiKeyValidator = apiKeyValidator;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String apiKey = request.getHeader(SecurityConstants.API_KEY_HEADER);
        if (apiKey == null || SecurityContextHolder.getContext().getAuthentication() != null) {
            filterChain.doFilter(request, response);
            return;
        }

        Optional<ApiKeyValidator.ApiKeyInfo> keyInfo = apiKeyValidator.validate(apiKey);
        if (keyInfo.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }

        ApiKeyValidator.ApiKeyInfo info = keyInfo.get();
        var authorities = info.permissions().stream()
                .map(SimpleGrantedAuthority::new)
                .toList();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("apikey:" + info.keyType(), null, authorities));
        log.debug("Authenticated via API key for org={} space={}", info.orgId(), info.spaceId());

        try {
            // Tenant from API key metadata (userId is null for API keys), bound for the rest of the request
            TenantScopedChain.proceed(new TenantInfo(info.orgId(), info.spaceId(), null, SecurityConstants.ROLE_SOLVER),
                    filterChain, request, response);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}
