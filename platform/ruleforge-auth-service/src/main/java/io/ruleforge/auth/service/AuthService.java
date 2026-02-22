package io.ruleforge.auth.service;

import io.ruleforge.auth.dto.*;
import io.ruleforge.auth.entity.MemberEntity;
import io.ruleforge.auth.entity.OrganizationEntity;
import io.ruleforge.auth.entity.UserEntity;
import io.ruleforge.auth.repository.MemberRepository;
import io.ruleforge.auth.repository.OrganizationRepository;
import io.ruleforge.auth.repository.UserRepository;
import io.ruleforge.common.exception.ConflictException;
import io.ruleforge.common.security.JwtTokenProvider;
import io.ruleforge.common.security.SecurityConstants;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final MemberRepository memberRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository,
                       OrganizationRepository organizationRepository,
                       MemberRepository memberRepository,
                       JwtTokenProvider jwtTokenProvider,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.organizationRepository = organizationRepository;
        this.memberRepository = memberRepository;
        this.jwtTokenProvider = jwtTokenProvider;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new ConflictException("A user with this email already exists");
        }

        // Create user
        UserEntity user = new UserEntity();
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setFullName(request.fullName());
        user = userRepository.save(user);

        // Create default organization
        String orgName = (request.organizationName() != null && !request.organizationName().isBlank())
                ? request.organizationName()
                : request.fullName() + "'s Organization";
        String slug = generateSlug(orgName);
        OrganizationEntity org = new OrganizationEntity();
        org.setName(orgName);
        org.setSlug(slug);
        org = organizationRepository.save(org);

        // Add user as owner
        MemberEntity member = new MemberEntity();
        member.setOrgId(org.getId());
        member.setUserId(user.getId());
        member.setRole(SecurityConstants.ROLE_OWNER);
        memberRepository.save(member);

        log.info("Registered new user: {} with org: {}", user.getEmail(), org.getSlug());

        return buildAuthResponse(user, org.getId(), SecurityConstants.ROLE_OWNER);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        UserEntity user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid email or password");
        }

        // Get the first membership for default org context
        MemberEntity membership = memberRepository.findByUserId(user.getId())
                .stream()
                .findFirst()
                .orElseThrow(() -> new BadCredentialsException("User has no organization membership"));

        log.info("User logged in: {}", user.getEmail());

        return buildAuthResponse(user, membership.getOrgId(), membership.getRole());
    }

    @Transactional(readOnly = true)
    public AuthResponse refresh(RefreshRequest request) {
        if (!jwtTokenProvider.validateToken(request.refreshToken())) {
            throw new BadCredentialsException("Invalid or expired refresh token");
        }

        String tokenType = jwtTokenProvider.getTokenType(request.refreshToken());
        if (!"refresh".equals(tokenType)) {
            throw new BadCredentialsException("Token is not a refresh token");
        }

        UUID userId = jwtTokenProvider.getUserIdFromToken(request.refreshToken());
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new BadCredentialsException("User not found"));

        // Get the first membership for default org context
        MemberEntity membership = memberRepository.findByUserId(user.getId())
                .stream()
                .findFirst()
                .orElseThrow(() -> new BadCredentialsException("User has no organization membership"));

        return buildAuthResponse(user, membership.getOrgId(), membership.getRole());
    }

    private AuthResponse buildAuthResponse(UserEntity user, UUID orgId, String role) {
        String accessToken = jwtTokenProvider.generateAccessToken(
                user.getId(), orgId, user.getEmail(), role);
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId());

        UserDto userDto = new UserDto(user.getId(), user.getEmail(), user.getFullName());
        return new AuthResponse(accessToken, refreshToken, userDto);
    }

    private String generateSlug(String name) {
        String base = name.toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");
        if (base.length() < 3) {
            base = base + "-org";
        }
        // Ensure uniqueness
        String slug = base;
        int counter = 1;
        while (organizationRepository.existsBySlug(slug)) {
            slug = base + "-" + counter++;
        }
        return slug;
    }
}
