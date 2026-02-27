package io.ruleforge.management.service;

import io.ruleforge.common.exception.ConflictException;
import io.ruleforge.common.exception.EntityNotFoundException;
import io.ruleforge.management.dto.AbTestDto;
import io.ruleforge.management.dto.CreateAbTestRequest;
import io.ruleforge.management.dto.RuleVersionDto;
import io.ruleforge.management.entity.AbTestEntity;
import io.ruleforge.management.entity.SpaceEntity;
import io.ruleforge.management.repository.AbTestRepository;
import io.ruleforge.management.repository.SpaceRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class AbTestService {

    private static final Logger log = LoggerFactory.getLogger(AbTestService.class);

    private final AbTestRepository abTestRepository;
    private final SpaceRepository spaceRepository;
    private final VersionService versionService;
    private final StringRedisTemplate redisTemplate;

    public AbTestService(AbTestRepository abTestRepository,
                         SpaceRepository spaceRepository,
                         VersionService versionService,
                         StringRedisTemplate redisTemplate) {
        this.abTestRepository = abTestRepository;
        this.spaceRepository = spaceRepository;
        this.versionService = versionService;
        this.redisTemplate = redisTemplate;
    }

    @Transactional
    public AbTestDto create(UUID ruleId, UUID spaceId, UUID createdBy, CreateAbTestRequest req) {
        AbTestEntity entity = new AbTestEntity();
        entity.setRuleId(ruleId);
        entity.setSpaceId(spaceId);
        entity.setCreatedBy(createdBy);
        entity.setName(req.name());
        entity.setVersionA(req.versionA());
        entity.setVersionB(req.versionB());
        entity.setTrafficSplit(req.trafficSplit());
        entity.setStatus("PENDING");
        entity.setStartDate(req.startDate());
        entity.setEndDate(req.endDate());

        entity = abTestRepository.save(entity);
        log.info("A/B test created: id={}, ruleId={}, name={}", entity.getId(), ruleId, entity.getName());
        return toDto(entity);
    }

    @Transactional(readOnly = true)
    public List<AbTestDto> findAllForOrg(UUID orgId) {
        List<UUID> spaceIds = spaceRepository.findByOrgId(orgId).stream()
                .map(SpaceEntity::getId)
                .toList();
        if (spaceIds.isEmpty()) {
            return List.of();
        }
        return abTestRepository.findBySpaceIdIn(spaceIds).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AbTestDto> findByRuleId(UUID ruleId) {
        return abTestRepository.findByRuleId(ruleId).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public AbTestDto findById(UUID id) {
        AbTestEntity entity = findOrThrow(id);
        return toDto(entity);
    }

    @Transactional
    public AbTestDto start(UUID id) {
        AbTestEntity entity = findOrThrow(id);
        entity.setStatus("ACTIVE");
        entity = abTestRepository.save(entity);
        log.info("A/B test started: id={}", id);
        return toDto(entity);
    }

    @Transactional
    public AbTestDto stop(UUID id) {
        AbTestEntity entity = findOrThrow(id);
        entity.setStatus("STOPPED");
        entity = abTestRepository.save(entity);
        log.info("A/B test stopped: id={}", id);
        return toDto(entity);
    }

    @Transactional
    public AbTestDto delete(UUID id) {
        AbTestEntity entity = findOrThrow(id);
        AbTestDto dto = toDto(entity);
        abTestRepository.delete(entity);
        log.info("A/B test deleted: id={}", id);
        return dto;
    }

    @Transactional
    public RuleVersionDto promote(UUID ruleId, UUID testId, String winner) {
        AbTestEntity test = findOrThrow(testId);

        if (!test.getRuleId().equals(ruleId)) {
            throw new EntityNotFoundException("AbTest", testId.toString());
        }

        if (!"ACTIVE".equals(test.getStatus()) && !"STOPPED".equals(test.getStatus())) {
            throw new ConflictException("Only ACTIVE or STOPPED A/B tests can be promoted. Current status: %s"
                    .formatted(test.getStatus()));
        }

        int winningVersion;
        if ("A".equalsIgnoreCase(winner)) {
            winningVersion = test.getVersionA();
        } else if ("B".equalsIgnoreCase(winner)) {
            winningVersion = test.getVersionB();
        } else {
            throw new IllegalArgumentException("Winner must be 'A' or 'B', got: " + winner);
        }

        // Mark test as COMPLETED
        test.setStatus("COMPLETED");
        abTestRepository.save(test);

        // Publish the winning version
        RuleVersionDto promoted = versionService.publishVersion(ruleId, winningVersion);

        // Invalidate Redis caches
        try {
            redisTemplate.delete("ab_test:" + ruleId + ":active");
            Set<String> ruleKeys = redisTemplate.keys("tenant:*:rule:" + ruleId + ":*");
            if (ruleKeys != null && !ruleKeys.isEmpty()) {
                redisTemplate.delete(ruleKeys);
            }
        } catch (Exception e) {
            log.warn("Failed to invalidate Redis caches for ruleId={}: {}", ruleId, e.getMessage());
        }

        log.info("A/B test promoted: testId={}, winner={}, version={}", testId, winner, winningVersion);
        return promoted;
    }

    private AbTestEntity findOrThrow(UUID id) {
        return abTestRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("AbTest", id.toString()));
    }

    private AbTestDto toDto(AbTestEntity entity) {
        return new AbTestDto(
                entity.getId(),
                entity.getSpaceId(),
                entity.getRuleId(),
                entity.getName(),
                entity.getVersionA(),
                entity.getVersionB(),
                entity.getTrafficSplit(),
                entity.getStatus(),
                entity.getStartDate(),
                entity.getEndDate(),
                entity.getCreatedBy(),
                entity.getCreatedAt()
        );
    }
}
