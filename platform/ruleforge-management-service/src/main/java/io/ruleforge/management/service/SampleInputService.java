package io.ruleforge.management.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.ruleforge.common.exception.ConflictException;
import io.ruleforge.common.exception.EntityNotFoundException;
import io.ruleforge.common.tenant.TenantContext;
import io.ruleforge.management.dto.CreateSampleInputRequest;
import io.ruleforge.management.dto.SampleInputDto;
import io.ruleforge.management.dto.UpdateSampleInputRequest;
import io.ruleforge.management.entity.SampleInputEntity;
import io.ruleforge.management.repository.SampleInputRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class SampleInputService {

    private static final Logger log = LoggerFactory.getLogger(SampleInputService.class);

    private final SampleInputRepository sampleInputRepository;
    private final RuleService ruleService;
    private final ObjectMapper objectMapper;

    public SampleInputService(SampleInputRepository sampleInputRepository,
                              RuleService ruleService,
                              ObjectMapper objectMapper) {
        this.sampleInputRepository = sampleInputRepository;
        this.ruleService = ruleService;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<SampleInputDto> listSampleInputs(UUID ruleId) {
        // Verify tenant owns this rule
        ruleService.getRule(ruleId);
        return sampleInputRepository.findByRuleIdOrderBySortOrder(ruleId).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public SampleInputDto createSampleInput(UUID ruleId, CreateSampleInputRequest request) {
        // Verify tenant owns this rule
        ruleService.getRule(ruleId);

        // Check name uniqueness within rule
        sampleInputRepository.findByRuleIdAndName(ruleId, request.name()).ifPresent(existing -> {
            throw new ConflictException("Sample input with name '%s' already exists for this rule".formatted(request.name()));
        });

        SampleInputEntity entity = new SampleInputEntity();
        entity.setRuleId(ruleId);
        entity.setName(request.name());
        entity.setDescription(request.description());
        entity.setInput(serializeInput(request.input()));
        entity.setSortOrder(sampleInputRepository.countByRuleId(ruleId));
        entity.setCreatedBy(TenantContext.get().userId());

        entity = sampleInputRepository.save(entity);
        log.info("Sample input created: id={}, ruleId={}, name={}", entity.getId(), ruleId, request.name());

        return toDto(entity);
    }

    @Transactional
    public SampleInputDto updateSampleInput(UUID ruleId, UUID sampleId, UpdateSampleInputRequest request) {
        // Verify tenant owns this rule
        ruleService.getRule(ruleId);

        SampleInputEntity entity = sampleInputRepository.findById(sampleId)
                .orElseThrow(() -> new EntityNotFoundException("SampleInput", sampleId.toString()));

        if (!entity.getRuleId().equals(ruleId)) {
            throw new EntityNotFoundException("SampleInput", sampleId.toString());
        }

        if (request.name() != null) {
            // Check name uniqueness if changing name
            if (!request.name().equals(entity.getName())) {
                sampleInputRepository.findByRuleIdAndName(ruleId, request.name()).ifPresent(existing -> {
                    throw new ConflictException("Sample input with name '%s' already exists for this rule".formatted(request.name()));
                });
            }
            entity.setName(request.name());
        }
        if (request.description() != null) {
            entity.setDescription(request.description());
        }
        if (request.input() != null) {
            entity.setInput(serializeInput(request.input()));
        }
        if (request.sortOrder() != null) {
            entity.setSortOrder(request.sortOrder());
        }

        entity = sampleInputRepository.save(entity);
        log.info("Sample input updated: id={}, ruleId={}", sampleId, ruleId);

        return toDto(entity);
    }

    @Transactional
    public void deleteSampleInput(UUID ruleId, UUID sampleId) {
        // Verify tenant owns this rule
        ruleService.getRule(ruleId);

        SampleInputEntity entity = sampleInputRepository.findById(sampleId)
                .orElseThrow(() -> new EntityNotFoundException("SampleInput", sampleId.toString()));

        if (!entity.getRuleId().equals(ruleId)) {
            throw new EntityNotFoundException("SampleInput", sampleId.toString());
        }

        sampleInputRepository.delete(entity);
        log.info("Sample input deleted: id={}, ruleId={}", sampleId, ruleId);
    }

    private String serializeInput(Object input) {
        try {
            return objectMapper.writeValueAsString(input);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Invalid input JSON", e);
        }
    }

    private SampleInputDto toDto(SampleInputEntity entity) {
        return new SampleInputDto(entity.getId(), entity.getName(), entity.getDescription(),
                entity.getInput(), entity.getSortOrder());
    }
}
