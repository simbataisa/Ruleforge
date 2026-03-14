package io.ruleforge.auth.service;

import io.ruleforge.auth.dto.CreateSpaceRequest;
import io.ruleforge.auth.dto.SpaceDto;
import io.ruleforge.auth.dto.UpdateSpaceRequest;
import io.ruleforge.auth.entity.SpaceEntity;
import io.ruleforge.auth.repository.SpaceRepository;
import io.ruleforge.auth.util.SlugUtils;
import io.ruleforge.common.exception.ConflictException;
import io.ruleforge.common.exception.EntityNotFoundException;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class SpaceService {

    private final SpaceRepository spaceRepository;
    private final OrganizationService organizationService;

    public SpaceService(SpaceRepository spaceRepository,
                        OrganizationService organizationService) {
        this.spaceRepository = spaceRepository;
        this.organizationService = organizationService;
    }

    @Transactional
    public SpaceDto createSpace(UUID orgId, UUID userId, CreateSpaceRequest request) {
        organizationService.ensureAdminOrOwner(orgId, userId);

        SpaceEntity space = new SpaceEntity();
        space.setOrgId(orgId);
        space.setName(request.name());
        space.setSlug(SlugUtils.toSlug(request.name()));

        try {
            space = spaceRepository.save(space);
        } catch (DataIntegrityViolationException ex) {
            if (ex.getMessage() != null && ex.getMessage().contains("spaces_slug_org_unique")) {
                throw new ConflictException("A space with this name already exists in your organization");
            }
            throw ex;
        }

        return toDto(space, 0L);
    }

    @Transactional(readOnly = true)
    public List<SpaceDto> listSpaces(UUID orgId, UUID userId) {
        organizationService.ensureMember(orgId, userId);

        List<SpaceEntity> spaces = spaceRepository.findByOrgId(orgId);
        if (spaces.isEmpty()) {
            return List.of();
        }

        // Bulk count query to avoid N+1
        List<UUID> spaceIds = spaces.stream().map(SpaceEntity::getId).toList();
        Map<UUID, Long> countBySpaceId = buildCountMap(spaceIds);

        return spaces.stream()
                .map(s -> toDto(s, countBySpaceId.getOrDefault(s.getId(), 0L)))
                .toList();
    }

    @Transactional(readOnly = true)
    public SpaceDto getSpace(UUID orgId, UUID spaceId, UUID userId) {
        organizationService.ensureMember(orgId, userId);

        SpaceEntity space = spaceRepository.findById(spaceId)
                .filter(s -> s.getOrgId().equals(orgId))
                .orElseThrow(() -> new EntityNotFoundException("Space", spaceId.toString()));

        long ruleCount = spaceRepository.countRulesBySpaceId(spaceId);
        return toDto(space, ruleCount);
    }

    @Transactional
    public SpaceDto updateSpace(UUID orgId, UUID spaceId, UUID userId, UpdateSpaceRequest request) {
        organizationService.ensureAdminOrOwner(orgId, userId);

        SpaceEntity space = spaceRepository.findById(spaceId)
                .filter(s -> s.getOrgId().equals(orgId))
                .orElseThrow(() -> new EntityNotFoundException("Space", spaceId.toString()));

        space.setName(request.name());
        space.setSlug(SlugUtils.toSlug(request.name()));
        space.setDescription(request.description());

        try {
            space = spaceRepository.save(space);
        } catch (DataIntegrityViolationException ex) {
            if (ex.getMessage() != null && ex.getMessage().contains("spaces_slug_org_unique")) {
                throw new ConflictException("A space with this name already exists in your organization");
            }
            throw ex;
        }

        long ruleCount = spaceRepository.countRulesBySpaceId(spaceId);
        return toDto(space, ruleCount);
    }

    @Transactional
    public void deleteSpace(UUID orgId, UUID spaceId, UUID userId) {
        organizationService.ensureOwner(orgId, userId);

        SpaceEntity space = spaceRepository.findById(spaceId)
                .filter(s -> s.getOrgId().equals(orgId))
                .orElseThrow(() -> new EntityNotFoundException("Space", spaceId.toString()));

        long ruleCount = spaceRepository.countRulesBySpaceId(spaceId);
        if (ruleCount > 0) {
            throw new ConflictException(
                String.format("This space contains %d rule%s and cannot be deleted. Remove all rules first.",
                    ruleCount, ruleCount == 1 ? "" : "s")
            );
        }

        spaceRepository.delete(space);
    }

    private Map<UUID, Long> buildCountMap(List<UUID> spaceIds) {
        List<Object[]> results = spaceRepository.countRulesBySpaceIds(spaceIds);
        Map<UUID, Long> map = new HashMap<>();
        for (Object[] row : results) {
            UUID spaceId = (UUID) row[0];
            Long count = ((Number) row[1]).longValue();
            map.put(spaceId, count);
        }
        return map;
    }

    private SpaceDto toDto(SpaceEntity space, long ruleCount) {
        return new SpaceDto(
                space.getId(),
                space.getName(),
                space.getSlug(),
                space.getDescription(),
                ruleCount,
                space.getOrgId(),
                space.getCreatedAt(),
                space.getUpdatedAt()
        );
    }
}
