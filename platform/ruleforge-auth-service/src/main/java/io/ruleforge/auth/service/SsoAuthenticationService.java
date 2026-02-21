package io.ruleforge.auth.service;

import io.ruleforge.auth.dto.AuthResponse;
import io.ruleforge.auth.dto.UserDto;
import io.ruleforge.auth.entity.MemberEntity;
import io.ruleforge.auth.entity.SsoConfigEntity;
import io.ruleforge.auth.entity.SsoProviderType;
import io.ruleforge.auth.entity.UserEntity;
import io.ruleforge.auth.repository.MemberRepository;
import io.ruleforge.auth.repository.SsoConfigRepository;
import io.ruleforge.auth.repository.UserRepository;
import io.ruleforge.common.security.JwtTokenProvider;
import io.ruleforge.common.security.SecurityConstants;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
public class SsoAuthenticationService {

    private static final Logger log = LoggerFactory.getLogger(SsoAuthenticationService.class);

    private final SsoConfigRepository ssoConfigRepository;
    private final UserRepository userRepository;
    private final MemberRepository memberRepository;
    private final JwtTokenProvider jwtTokenProvider;

    public SsoAuthenticationService(SsoConfigRepository ssoConfigRepository,
                                     UserRepository userRepository,
                                     MemberRepository memberRepository,
                                     JwtTokenProvider jwtTokenProvider) {
        this.ssoConfigRepository = ssoConfigRepository;
        this.userRepository = userRepository;
        this.memberRepository = memberRepository;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Transactional
    public AuthResponse processSamlResponse(String samlResponse, String relayState) {
        // Decode the SAML response to extract user attributes
        // In production this would use OpenSAML to parse and validate the assertion
        Map<String, String> attributes = decodeSamlAttributes(samlResponse);

        String email = attributes.get("email");
        String fullName = attributes.getOrDefault("name", email);
        String orgIdStr = relayState;

        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("SAML response missing required email attribute");
        }

        UUID orgId = UUID.fromString(orgIdStr);
        SsoConfigEntity config = findEnabledConfig(orgId, SsoProviderType.SAML);

        return findOrCreateUserAndAuthenticate(email, fullName, orgId, config);
    }

    @Transactional
    public AuthResponse processOidcCallback(String code, String state) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("OIDC callback missing authorization code");
        }

        // Parse state to extract org ID
        UUID orgId = UUID.fromString(state);
        SsoConfigEntity config = findEnabledConfig(orgId, SsoProviderType.OIDC);

        // Exchange authorization code for tokens at the provider's token endpoint
        // In production this would call the OIDC provider's token endpoint
        Map<String, String> userInfo = exchangeCodeForUserInfo(code, config);

        String email = userInfo.get("email");
        String fullName = userInfo.getOrDefault("name", email);

        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("OIDC token response missing required email claim");
        }

        return findOrCreateUserAndAuthenticate(email, fullName, orgId, config);
    }

    public String generateSamlMetadata(UUID orgId) {
        SsoConfigEntity config = ssoConfigRepository.findByOrgId(orgId).stream()
                .filter(c -> c.getProviderType() == SsoProviderType.SAML && c.isEnabled())
                .findFirst()
                .orElse(null);

        String entityId = "urn:ruleforge:sp";
        String acsUrl = "/v1/sso/saml/acs";

        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\"?>\n");
        sb.append("<md:EntityDescriptor xmlns:md=\"urn:oasis:names:tc:SAML:2.0:metadata\" entityID=\"").append(entityId).append("\">\n");
        sb.append("  <md:SPSSODescriptor protocolSupportEnumeration=\"urn:oasis:names:tc:SAML:2.0:protocol\">\n");
        sb.append("    <md:NameIDFormat>urn:oasis:names:tc:SAML:1.1:nameid-format:emailAddress</md:NameIDFormat>\n");
        sb.append("    <md:AssertionConsumerService Binding=\"urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST\" Location=\"").append(acsUrl).append("\" index=\"0\"/>\n");
        sb.append("  </md:SPSSODescriptor>\n");
        sb.append("</md:EntityDescriptor>");
        return sb.toString();
    }

    private SsoConfigEntity findEnabledConfig(UUID orgId, SsoProviderType type) {
        return ssoConfigRepository.findByOrgId(orgId).stream()
                .filter(c -> c.getProviderType() == type && c.isEnabled())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "No enabled %s SSO configuration found for org %s".formatted(type, orgId)));
    }

    private AuthResponse findOrCreateUserAndAuthenticate(String email, String fullName, UUID orgId,
                                                          SsoConfigEntity config) {
        // Find or create user
        UserEntity user = userRepository.findByEmail(email).orElseGet(() -> {
            UserEntity newUser = new UserEntity();
            newUser.setEmail(email);
            newUser.setFullName(fullName);
            // SSO users don't need a password
            newUser.setPasswordHash("SSO_AUTHENTICATED");
            return userRepository.save(newUser);
        });

        // Ensure membership exists for this org
        String role = memberRepository.findByUserId(user.getId()).stream()
                .filter(m -> m.getOrgId().equals(orgId))
                .map(MemberEntity::getRole)
                .findFirst()
                .orElseGet(() -> {
                    MemberEntity member = new MemberEntity();
                    member.setOrgId(orgId);
                    member.setUserId(user.getId());
                    member.setRole(SecurityConstants.ROLE_VIEWER);
                    memberRepository.save(member);
                    return SecurityConstants.ROLE_VIEWER;
                });

        log.info("SSO authentication successful: email={}, orgId={}, provider={}", email, orgId, config.getProviderType());

        String accessToken = jwtTokenProvider.generateAccessToken(user.getId(), orgId, user.getEmail(), role);
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId());

        UserDto userDto = new UserDto(user.getId(), user.getEmail(), user.getFullName());
        return new AuthResponse(accessToken, refreshToken, userDto);
    }

    private Map<String, String> decodeSamlAttributes(String samlResponse) {
        // In production: use OpenSAML to parse XML, validate signature, extract attributes
        // For now, decode base64 and extract minimal info
        try {
            byte[] decoded = java.util.Base64.getDecoder().decode(samlResponse);
            String xml = new String(decoded);
            // Minimal extraction - production would use proper XML parsing
            String email = extractXmlValue(xml, "email");
            String name = extractXmlValue(xml, "name");
            return Map.of("email", email != null ? email : "", "name", name != null ? name : "");
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to decode SAML response: " + e.getMessage(), e);
        }
    }

    private String extractXmlValue(String xml, String attributeName) {
        // Simple attribute extraction - production code should use proper XML parser
        int idx = xml.indexOf("Name=\"" + attributeName + "\"");
        if (idx < 0) return null;
        int valueStart = xml.indexOf("<saml:AttributeValue", idx);
        if (valueStart < 0) return null;
        int contentStart = xml.indexOf(">", valueStart);
        if (contentStart < 0) return null;
        int contentEnd = xml.indexOf("</saml:AttributeValue>", contentStart);
        if (contentEnd < 0) return null;
        return xml.substring(contentStart + 1, contentEnd).trim();
    }

    private Map<String, String> exchangeCodeForUserInfo(String code, SsoConfigEntity config) {
        // In production: use RestTemplate/WebClient to call the OIDC provider's token endpoint
        // with the authorization code, then call the userinfo endpoint
        // For now, this is a placeholder that would be replaced with actual HTTP calls
        throw new UnsupportedOperationException(
                "OIDC token exchange requires HTTP client integration. Configure the OIDC provider's token endpoint URL.");
    }
}
