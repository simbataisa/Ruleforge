package io.ruleforge.auth.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.ruleforge.auth.dto.ApiKeyCreatedDto;
import io.ruleforge.auth.dto.ApiKeyDto;
import io.ruleforge.auth.dto.CreateApiKeyRequest;
import io.ruleforge.auth.entity.ApiKeyEntity;
import io.ruleforge.auth.entity.SpaceEntity;
import io.ruleforge.auth.repository.ApiKeyRepository;
import io.ruleforge.auth.repository.SpaceRepository;
import io.ruleforge.common.exception.EntityNotFoundException;
import io.ruleforge.common.security.ApiKeyValidator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.*;

@Service
public class ApiKeyService implements ApiKeyValidator {

    private static final Logger log = LoggerFactory.getLogger(ApiKeyService.class);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final String CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    private final ApiKeyRepository apiKeyRepository;
    private final SpaceRepository spaceRepository;
    private final ObjectMapper objectMapper;

    public ApiKeyService(ApiKeyRepository apiKeyRepository,
                         SpaceRepository spaceRepository,
                         ObjectMapper objectMapper) {
        this.apiKeyRepository = apiKeyRepository;
        this.spaceRepository = spaceRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public ApiKeyCreatedDto createApiKey(UUID orgId, UUID spaceId, CreateApiKeyRequest request) {
        SpaceEntity space = spaceRepository.findById(spaceId)
                .filter(s -> s.getOrgId().equals(orgId))
                .orElseThrow(() -> new EntityNotFoundException("Space", spaceId.toString()));

        // Generate key: rf_<type>_<random32chars>
        String typePrefix = "SOLVER".equals(request.keyType()) ? "rf_slv_" : "rf_mgt_";
        String randomPart = generateRandomString(32);
        String rawKey = typePrefix + randomPart;
        String keyPrefix = typePrefix + randomPart.substring(0, 8);

        // Hash the key for storage
        String keyHash = sha256(rawKey);

        // Serialize permissions to JSON
        String permissionsJson;
        try {
            permissionsJson = objectMapper.writeValueAsString(
                    request.permissions() != null ? request.permissions() : List.of());
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize permissions", e);
        }

        ApiKeyEntity entity = new ApiKeyEntity();
        entity.setSpaceId(spaceId);
        entity.setOrgId(orgId);
        entity.setKeyHash(keyHash);
        entity.setKeyPrefix(keyPrefix);
        entity.setKeyType(request.keyType());
        entity.setPermissions(permissionsJson);
        entity.setName(request.name());
        entity.setExpiresAt(request.expiresAt());
        entity.setActive(true);
        entity = apiKeyRepository.save(entity);

        log.info("Created API key '{}' (prefix: {}) for space {}", request.name(), keyPrefix, spaceId);

        return new ApiKeyCreatedDto(entity.getId(), rawKey, keyPrefix, entity.getName());
    }

    @Transactional(readOnly = true)
    public List<ApiKeyDto> listApiKeys(UUID spaceId) {
        return apiKeyRepository.findBySpaceId(spaceId).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ApiKeyDto> listApiKeysForOrg(UUID orgId) {
        return apiKeyRepository.findByOrgId(orgId).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public void revokeApiKey(UUID keyId) {
        ApiKeyEntity entity = apiKeyRepository.findById(keyId)
                .orElseThrow(() -> new EntityNotFoundException("ApiKey", keyId.toString()));

        entity.setActive(false);
        apiKeyRepository.save(entity);

        log.info("Revoked API key: {} (prefix: {})", keyId, entity.getKeyPrefix());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ApiKeyInfo> validate(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            return Optional.empty();
        }

        // Extract prefix: rf_slv_ or rf_mgt_ + first 8 chars of random part
        if (apiKey.length() < 15) {
            return Optional.empty();
        }

        String typePrefix = apiKey.substring(0, 7); // rf_slv_ or rf_mgt_
        String keyPrefix = typePrefix + apiKey.substring(7, 15);

        Optional<ApiKeyEntity> entityOpt = apiKeyRepository.findByKeyPrefix(keyPrefix);
        if (entityOpt.isEmpty()) {
            return Optional.empty();
        }

        ApiKeyEntity entity = entityOpt.get();

        // Check active status
        if (!entity.isActive()) {
            return Optional.empty();
        }

        // Check expiration
        if (entity.getExpiresAt() != null && entity.getExpiresAt().isBefore(Instant.now())) {
            return Optional.empty();
        }

        // Verify hash
        String hash = sha256(apiKey);
        if (!hash.equals(entity.getKeyHash())) {
            return Optional.empty();
        }

        List<String> permissions = parsePermissions(entity.getPermissions());

        return Optional.of(new ApiKeyInfo(
                entity.getOrgId(),
                entity.getSpaceId(),
                entity.getKeyType(),
                permissions
        ));
    }

    private ApiKeyDto toDto(ApiKeyEntity entity) {
        List<String> permissions = parsePermissions(entity.getPermissions());
        return new ApiKeyDto(
                entity.getId(),
                entity.getName(),
                entity.getKeyPrefix(),
                entity.getKeyType(),
                permissions,
                entity.isActive(),
                entity.getExpiresAt(),
                entity.getCreatedAt()
        );
    }

    private List<String> parsePermissions(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse permissions JSON: {}", json, e);
            return List.of();
        }
    }

    private static String generateRandomString(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(CHARS.charAt(SECURE_RANDOM.nextInt(CHARS.length())));
        }
        return sb.toString();
    }

    private static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
