package io.ruleforge.common.security;

import io.ruleforge.common.tenant.TenantContext;
import io.ruleforge.common.tenant.TenantContext.TenantInfo;

import io.jsonwebtoken.Claims;
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
import java.util.List;
import java.util.UUID;

/**
 * Spring Web filter that extracts a JWT from the {@code Authorization: Bearer <token>}
 * header, validates it, and populates both {@link TenantContext} and Spring's
 * {@link SecurityContextHolder}.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtTokenProvider jwtTokenProvider;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String token = resolveToken(request);
            if (token == null || !jwtTokenProvider.validateToken(token)) {
                filterChain.doFilter(request, response);
                return;
            }

            Claims claims = jwtTokenProvider.parseToken(token);
            UUID userId = UUID.fromString(claims.getSubject());
            UUID orgId = UUID.fromString(claims.get("orgId", String.class));
            String role = claims.get("role", String.class);

            // Set Spring Security context
            var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role));
            var authentication = new UsernamePasswordAuthenticationToken(userId, null, authorities);
            SecurityContextHolder.getContext().setAuthentication(authentication);

            // Tenant bound for the rest of the request (spaceId is null for JWT — it is request-scoped)
            TenantScopedChain.proceed(new TenantInfo(orgId, null, userId, role), filterChain, request, response);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader(SecurityConstants.HEADER_STRING);
        if (header != null && header.startsWith(SecurityConstants.TOKEN_PREFIX)) {
            return header.substring(SecurityConstants.TOKEN_PREFIX.length());
        }
        // Fallback: query param for SSE/EventSource connections that cannot set headers
        String queryToken = request.getParameter("token");
        if (queryToken != null && !queryToken.isBlank()) {
            return queryToken;
        }
        return null;
    }
}
