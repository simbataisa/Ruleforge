package io.ruleforge.auth.service;

import io.ruleforge.auth.dto.CreateSpaceRequest;
import io.ruleforge.auth.dto.SpaceDto;
import io.ruleforge.auth.entity.SpaceEntity;
import io.ruleforge.auth.repository.SpaceRepository;
import io.ruleforge.common.exception.EntityNotFoundException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
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
        space = spaceRepository.save(space);

        return toDto(space);
    }

    @Transactional(readOnly = true)
    public List<SpaceDto> listSpaces(UUID orgId, UUID userId) {
        organizationService.ensureMember(orgId, userId);

        return spaceRepository.findByOrgId(orgId).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public SpaceDto getSpace(UUID orgId, UUID spaceId, UUID userId) {
        organizationService.ensureMember(orgId, userId);

        SpaceEntity space = spaceRepository.findById(spaceId)
                .filter(s -> s.getOrgId().equals(orgId))
                .orElseThrow(() -> new EntityNotFoundException("Space", spaceId.toString()));

        return toDto(space);
    }

    private SpaceDto toDto(SpaceEntity space) {
        return new SpaceDto(space.getId(), space.getName(), space.getOrgId(), space.getCreatedAt());
    }
}
