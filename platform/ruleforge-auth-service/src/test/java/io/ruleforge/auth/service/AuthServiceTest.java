package io.ruleforge.auth.service;

import io.ruleforge.auth.dto.AuthResponse;
import io.ruleforge.auth.dto.LoginRequest;
import io.ruleforge.auth.dto.RegisterRequest;
import io.ruleforge.auth.entity.MemberEntity;
import io.ruleforge.auth.entity.OrganizationEntity;
import io.ruleforge.auth.entity.UserEntity;
import io.ruleforge.auth.repository.MemberRepository;
import io.ruleforge.auth.repository.OrganizationRepository;
import io.ruleforge.auth.repository.UserRepository;
import io.ruleforge.common.exception.ConflictException;
import io.ruleforge.common.security.JwtTokenProvider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private OrganizationRepository organizationRepository;
    @Mock private MemberRepository memberRepository;
    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private PasswordEncoder passwordEncoder;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, organizationRepository,
                memberRepository, jwtTokenProvider, passwordEncoder);
    }

    @Test
    void register_createsUserOrgAndMember() {
        RegisterRequest request = new RegisterRequest("user@test.com", "password123", "Test User");

        when(userRepository.existsByEmail("user@test.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashed");

        UserEntity savedUser = new UserEntity();
        savedUser.setId(UUID.randomUUID());
        savedUser.setEmail("user@test.com");
        savedUser.setFullName("Test User");
        when(userRepository.save(any(UserEntity.class))).thenReturn(savedUser);

        OrganizationEntity savedOrg = new OrganizationEntity();
        savedOrg.setId(UUID.randomUUID());
        savedOrg.setSlug("test-user");
        when(organizationRepository.save(any(OrganizationEntity.class))).thenReturn(savedOrg);
        when(organizationRepository.existsBySlug(anyString())).thenReturn(false);

        when(memberRepository.save(any(MemberEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        when(jwtTokenProvider.generateAccessToken(any(), any(), anyString(), anyString()))
                .thenReturn("access-token");
        when(jwtTokenProvider.generateRefreshToken(any())).thenReturn("refresh-token");

        AuthResponse response = authService.register(request);

        assertNotNull(response);
        assertEquals("access-token", response.accessToken());
        assertEquals("refresh-token", response.refreshToken());
        assertEquals("user@test.com", response.user().email());

        verify(userRepository).save(any(UserEntity.class));
        verify(organizationRepository).save(any(OrganizationEntity.class));
        verify(memberRepository).save(any(MemberEntity.class));
    }

    @Test
    void register_throwsConflictWhenEmailExists() {
        RegisterRequest request = new RegisterRequest("existing@test.com", "password123", "Test User");
        when(userRepository.existsByEmail("existing@test.com")).thenReturn(true);

        assertThrows(ConflictException.class, () -> authService.register(request));
        verify(userRepository, never()).save(any());
    }

    @Test
    void login_returnsTokensForValidCredentials() {
        LoginRequest request = new LoginRequest("user@test.com", "password123");

        UserEntity user = new UserEntity();
        user.setId(UUID.randomUUID());
        user.setEmail("user@test.com");
        user.setFullName("Test User");
        user.setPasswordHash("hashed");
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "hashed")).thenReturn(true);

        MemberEntity member = new MemberEntity();
        member.setOrgId(UUID.randomUUID());
        member.setRole("OWNER");
        when(memberRepository.findByUserId(user.getId())).thenReturn(List.of(member));

        when(jwtTokenProvider.generateAccessToken(any(), any(), anyString(), anyString()))
                .thenReturn("access-token");
        when(jwtTokenProvider.generateRefreshToken(any())).thenReturn("refresh-token");

        AuthResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("access-token", response.accessToken());
    }

    @Test
    void login_throwsBadCredentialsForWrongPassword() {
        LoginRequest request = new LoginRequest("user@test.com", "wrong");

        UserEntity user = new UserEntity();
        user.setId(UUID.randomUUID());
        user.setEmail("user@test.com");
        user.setPasswordHash("hashed");
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);

        assertThrows(BadCredentialsException.class, () -> authService.login(request));
    }

    @Test
    void login_throwsBadCredentialsForUnknownEmail() {
        LoginRequest request = new LoginRequest("unknown@test.com", "password123");
        when(userRepository.findByEmail("unknown@test.com")).thenReturn(Optional.empty());

        assertThrows(BadCredentialsException.class, () -> authService.login(request));
    }
}
