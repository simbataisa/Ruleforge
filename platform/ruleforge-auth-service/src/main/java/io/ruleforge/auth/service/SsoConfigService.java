package io.ruleforge.auth.service;

import io.ruleforge.auth.dto.CreateSsoConfigRequest;
import io.ruleforge.auth.dto.SsoConfigDto;
import io.ruleforge.auth.dto.UpdateSsoConfigRequest;
import io.ruleforge.auth.entity.SsoConfigEntity;
import io.ruleforge.auth.repository.SsoConfigRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class SsoConfigService {

    private final SsoConfigRepository ssoConfigRepository;

    public SsoConfigService(SsoConfigRepository ssoConfigRepository) {
        this.ssoConfigRepository = ssoConfigRepository;
    }

    public SsoConfigDto create(CreateSsoConfigRequest request) {
        SsoConfigEntity entity = new SsoConfigEntity();
        entity.setOrgId(request.orgId());
        entity.setProviderType(request.providerType());
        entity.setMetadataUrl(request.metadataUrl());
        entity.setClientId(request.clientId());
        if (request.clientSecret() != null) {
            entity.setClientSecretEncrypted(
                Base64.getEncoder().encodeToString(request.clientSecret().getBytes()));
        }
        if (request.attributeMapping() != null) {
            entity.setAttributeMapping(request.attributeMapping());
        }
        return toDto(ssoConfigRepository.save(entity));
    }

    @Transactional(readOnly = true)
    public List<SsoConfigDto> findByOrgId(UUID orgId) {
        return ssoConfigRepository.findByOrgId(orgId).stream()
            .map(this::toDto)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public SsoConfigDto findById(UUID id) {
        return ssoConfigRepository.findById(id)
            .map(this::toDto)
            .orElseThrow(() -> new RuntimeException("SSO config not found: " + id));
    }

    public SsoConfigDto update(UUID id, UpdateSsoConfigRequest request) {
        SsoConfigEntity entity = ssoConfigRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("SSO config not found: " + id));
        if (request.metadataUrl() != null) {
            entity.setMetadataUrl(request.metadataUrl());
        }
        if (request.clientId() != null) {
            entity.setClientId(request.clientId());
        }
        if (request.clientSecret() != null) {
            entity.setClientSecretEncrypted(
                Base64.getEncoder().encodeToString(request.clientSecret().getBytes()));
        }
        if (request.attributeMapping() != null) {
            entity.setAttributeMapping(request.attributeMapping());
        }
        if (request.enabled() != null) {
            entity.setEnabled(request.enabled());
        }
        return toDto(ssoConfigRepository.save(entity));
    }

    public void delete(UUID id) {
        ssoConfigRepository.deleteById(id);
    }

    private SsoConfigDto toDto(SsoConfigEntity e) {
        return new SsoConfigDto(
            e.getId(),
            e.getOrgId(),
            e.getProviderType(),
            e.getMetadataUrl(),
            e.getClientId(),
            e.getAttributeMapping(),
            e.isEnabled(),
            e.getCreatedAt(),
            e.getUpdatedAt()
        );
    }
}
