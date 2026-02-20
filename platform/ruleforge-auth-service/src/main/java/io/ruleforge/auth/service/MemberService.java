package io.ruleforge.auth.service;

import io.ruleforge.auth.dto.AddMemberRequest;
import io.ruleforge.auth.dto.MemberDto;
import io.ruleforge.auth.entity.MemberEntity;
import io.ruleforge.auth.entity.UserEntity;
import io.ruleforge.auth.repository.MemberRepository;
import io.ruleforge.auth.repository.UserRepository;
import io.ruleforge.common.exception.ConflictException;
import io.ruleforge.common.exception.EntityNotFoundException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class MemberService {

    private final MemberRepository memberRepository;
    private final UserRepository userRepository;
    private final OrganizationService organizationService;

    public MemberService(MemberRepository memberRepository,
                         UserRepository userRepository,
                         OrganizationService organizationService) {
        this.memberRepository = memberRepository;
        this.userRepository = userRepository;
        this.organizationService = organizationService;
    }

    @Transactional
    public MemberDto addMember(UUID orgId, UUID requesterId, AddMemberRequest request) {
        organizationService.ensureAdminOrOwner(orgId, requesterId);

        UserEntity user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new EntityNotFoundException("User", request.email()));

        if (memberRepository.existsByOrgIdAndUserId(orgId, user.getId())) {
            throw new ConflictException("User is already a member of this organization");
        }

        MemberEntity member = new MemberEntity();
        member.setOrgId(orgId);
        member.setUserId(user.getId());
        member.setRole(request.role());
        member = memberRepository.save(member);

        return toDto(member, user);
    }

    @Transactional
    public void removeMember(UUID orgId, UUID memberId, UUID requesterId) {
        organizationService.ensureAdminOrOwner(orgId, requesterId);

        MemberEntity member = memberRepository.findById(memberId)
                .filter(m -> m.getOrgId().equals(orgId))
                .orElseThrow(() -> new EntityNotFoundException("Member", memberId.toString()));

        memberRepository.delete(member);
    }

    @Transactional(readOnly = true)
    public List<MemberDto> listMembers(UUID orgId, UUID requesterId) {
        organizationService.ensureMember(orgId, requesterId);

        List<MemberEntity> members = memberRepository.findByOrgId(orgId);
        List<UUID> userIds = members.stream().map(MemberEntity::getUserId).toList();

        Map<UUID, UserEntity> usersById = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(UserEntity::getId, Function.identity()));

        return members.stream()
                .map(m -> toDto(m, usersById.get(m.getUserId())))
                .toList();
    }

    @Transactional
    public MemberDto updateRole(UUID orgId, UUID memberId, String role, UUID requesterId) {
        organizationService.ensureAdminOrOwner(orgId, requesterId);

        MemberEntity member = memberRepository.findById(memberId)
                .filter(m -> m.getOrgId().equals(orgId))
                .orElseThrow(() -> new EntityNotFoundException("Member", memberId.toString()));

        member.setRole(role);
        MemberEntity savedMember = memberRepository.save(member);

        UserEntity user = userRepository.findById(savedMember.getUserId())
                .orElseThrow(() -> new EntityNotFoundException("User", savedMember.getUserId().toString()));

        return toDto(savedMember, user);
    }

    private MemberDto toDto(MemberEntity member, UserEntity user) {
        return new MemberDto(
                member.getId(),
                member.getUserId(),
                user != null ? user.getEmail() : null,
                user != null ? user.getFullName() : null,
                member.getRole(),
                member.getCreatedAt()
        );
    }
}
