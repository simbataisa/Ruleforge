package io.ruleforge.auth.service;

import io.ruleforge.auth.dto.UserDto;
import io.ruleforge.auth.entity.UserEntity;
import io.ruleforge.auth.repository.MemberRepository;
import io.ruleforge.auth.repository.UserRepository;
import io.ruleforge.common.exception.EntityNotFoundException;
import io.ruleforge.common.tenant.TenantContext;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, MemberRepository memberRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.memberRepository = memberRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public UserDto getCurrentUser(UUID userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User", userId.toString()));
        String role = resolveRole(userId);
        return toDto(user, role);
    }

    @Transactional
    public UserDto updateProfile(UUID userId, String fullName) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User", userId.toString()));

        if (fullName != null && !fullName.isBlank()) {
            user.setFullName(fullName);
        }

        user = userRepository.save(user);
        String role = resolveRole(userId);
        return toDto(user, role);
    }

    @Transactional
    public void changePassword(UUID userId, String currentPassword, String newPassword) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User", userId.toString()));

        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new BadCredentialsException("Current password is incorrect");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    private String resolveRole(UUID userId) {
        // Role is in TenantContext (populated by JWT filter) — fall back to DB lookup
        TenantContext.TenantInfo tenant = TenantContext.get();
        if (tenant != null && tenant.role() != null) {
            return tenant.role();
        }
        // Fallback: look up member record using orgId from context
        if (tenant != null && tenant.orgId() != null) {
            return memberRepository.findByOrgIdAndUserId(tenant.orgId(), userId)
                    .map(m -> m.getRole())
                    .orElse(null);
        }
        return null;
    }

    private UserDto toDto(UserEntity user, String role) {
        return new UserDto(user.getId(), user.getEmail(), user.getFullName(), role);
    }
}
