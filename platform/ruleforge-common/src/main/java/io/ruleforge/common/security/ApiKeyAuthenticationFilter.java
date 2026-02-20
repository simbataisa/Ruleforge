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
        try {
            String apiKey = request.getHeader(SecurityConstants.API_KEY_HEADER);

            if (apiKey != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                Optional<ApiKeyValidator.ApiKeyInfo> keyInfo = apiKeyValidator.validate(apiKey);

                keyInfo.ifPresent(info -> {
                    // Set multi-tenant context from API key metadata (userId is null for API keys)
                    TenantContext.set(new TenantInfo(info.orgId(), info.spaceId(), null, SecurityConstants.ROLE_SOLVER));

                    // Build authorities from the key's permissions
                    var authorities = info.permissions().stream()
                            .map(SimpleGrantedAuthority::new)
                            .toList();

                    var authentication = new UsernamePasswordAuthenticationToken(
                            "apikey:" + info.keyType(), null, authorities);
                    SecurityContextHolder.getContext().setAuthentication(authentication);

                    log.debug("Authenticated via API key for org={} space={}", info.orgId(), info.spaceId());
                });
            }

            filterChain.doFilter(request, response);
        } finally {
            // Only clear if this filter set the context (check for apikey principal)
            var auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof String principal && principal.startsWith("apikey:")) {
                TenantContext.clear();
                SecurityContextHolder.clearContext();
            }
        }
    }
}
