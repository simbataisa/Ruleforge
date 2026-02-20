package io.ruleforge.auth.service;

import io.ruleforge.auth.dto.CreateOrganizationRequest;
import io.ruleforge.auth.dto.OrganizationDto;
import io.ruleforge.auth.entity.MemberEntity;
import io.ruleforge.auth.entity.OrganizationEntity;
import io.ruleforge.auth.repository.MemberRepository;
import io.ruleforge.auth.repository.OrganizationRepository;
import io.ruleforge.common.exception.ConflictException;
import io.ruleforge.common.exception.EntityNotFoundException;
import io.ruleforge.common.security.SecurityConstants;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final MemberRepository memberRepository;

    public OrganizationService(OrganizationRepository organizationRepository,
                                MemberRepository memberRepository) {
        this.organizationRepository = organizationRepository;
        this.memberRepository = memberRepository;
    }

    @Transactional
    public OrganizationDto createOrganization(UUID userId, CreateOrganizationRequest request) {
        if (organizationRepository.existsBySlug(request.slug())) {
            throw new ConflictException("Organization with slug '" + request.slug() + "' already exists");
        }

        OrganizationEntity org = new OrganizationEntity();
        org.setName(request.name());
        org.setSlug(request.slug());
        org = organizationRepository.save(org);

        // Add the creator as owner
        MemberEntity member = new MemberEntity();
        member.setOrgId(org.getId());
        member.setUserId(userId);
        member.setRole(SecurityConstants.ROLE_OWNER);
        memberRepository.save(member);

        return toDto(org);
    }

    @Transactional(readOnly = true)
    public List<OrganizationDto> getOrganizationsForUser(UUID userId) {
        List<MemberEntity> memberships = memberRepository.findByUserId(userId);
        List<UUID> orgIds = memberships.stream()
                .map(MemberEntity::getOrgId)
                .toList();

        return organizationRepository.findAllById(orgIds).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public OrganizationDto getOrganization(UUID orgId, UUID userId) {
        ensureMember(orgId, userId);
        OrganizationEntity org = organizationRepository.findById(orgId)
                .orElseThrow(() -> new EntityNotFoundException("Organization", orgId.toString()));
        return toDto(org);
    }

    @Transactional
    public OrganizationDto updateOrganization(UUID orgId, UUID userId, CreateOrganizationRequest request) {
        ensureAdminOrOwner(orgId, userId);

        OrganizationEntity org = organizationRepository.findById(orgId)
                .orElseThrow(() -> new EntityNotFoundException("Organization", orgId.toString()));

        // Check slug uniqueness if it changed
        if (!org.getSlug().equals(request.slug()) && organizationRepository.existsBySlug(request.slug())) {
            throw new ConflictException("Organization with slug '" + request.slug() + "' already exists");
        }

        org.setName(request.name());
        org.setSlug(request.slug());
        org = organizationRepository.save(org);

        return toDto(org);
    }

    public void ensureMember(UUID orgId, UUID userId) {
        if (!memberRepository.existsByOrgIdAndUserId(orgId, userId)) {
            throw new AccessDeniedException("You are not a member of this organization");
        }
    }

    public void ensureAdminOrOwner(UUID orgId, UUID userId) {
        MemberEntity member = memberRepository.findByOrgIdAndUserId(orgId, userId)
                .orElseThrow(() -> new AccessDeniedException("You are not a member of this organization"));

        if (!SecurityConstants.ROLE_OWNER.equals(member.getRole())
                && !SecurityConstants.ROLE_ADMIN.equals(member.getRole())) {
            throw new AccessDeniedException("You must be an OWNER or ADMIN to perform this action");
        }
    }

    private OrganizationDto toDto(OrganizationEntity org) {
        return new OrganizationDto(org.getId(), org.getName(), org.getSlug(), org.getCreatedAt());
    }
}
