package io.ruleforge.connector.service;

import io.ruleforge.connector.dto.*;
import io.ruleforge.connector.entity.ConnectorEntity;
import io.ruleforge.connector.repository.ConnectorRepository;
import io.ruleforge.connector.service.impl.HttpConnector;
import io.ruleforge.connector.service.impl.JdbcConnector;
import io.ruleforge.connector.service.impl.RedisConnector;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class ConnectorService {

    private final ConnectorRepository repository;
    private final CredentialEncryptionService encryptionService;
    private final JdbcConnector jdbcConnector;
    private final HttpConnector httpConnector;
    private final RedisConnector redisConnector;

    public ConnectorService(ConnectorRepository repository,
                             CredentialEncryptionService encryptionService,
                             JdbcConnector jdbcConnector,
                             HttpConnector httpConnector,
                             RedisConnector redisConnector) {
        this.repository = repository;
        this.encryptionService = encryptionService;
        this.jdbcConnector = jdbcConnector;
        this.httpConnector = httpConnector;
        this.redisConnector = redisConnector;
    }

    public ConnectorDto create(CreateConnectorRequest request) {
        ConnectorEntity entity = new ConnectorEntity();
        entity.setSpaceId(request.spaceId());
        entity.setName(request.name());
        entity.setType(request.type());
        entity.setConfig(request.config() != null ? request.config() : java.util.Map.of());
        entity.setDescription(request.description());
        entity.setCreatedBy(request.createdBy());
        if (request.credentials() != null) {
            entity.setCredentialsEncrypted(encryptionService.encrypt(request.credentials()));
        }
        return toDto(repository.save(entity));
    }

    @Transactional(readOnly = true)
    public ConnectorDto findById(UUID id) {
        return repository.findById(id)
                .map(this::toDto)
                .orElseThrow(() -> new RuntimeException("Connector not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<ConnectorDto> findBySpaceId(UUID spaceId) {
        return repository.findBySpaceId(spaceId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public ConnectorDto update(UUID id, UpdateConnectorRequest request) {
        ConnectorEntity entity = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Connector not found: " + id));
        if (request.name() != null) entity.setName(request.name());
        if (request.config() != null) entity.setConfig(request.config());
        if (request.credentials() != null) entity.setCredentialsEncrypted(encryptionService.encrypt(request.credentials()));
        if (request.enabled() != null) entity.setEnabled(request.enabled());
        if (request.description() != null) entity.setDescription(request.description());
        return toDto(repository.save(entity));
    }

    public void delete(UUID id) {
        repository.deleteById(id);
    }

    public TestConnectionResponse testConnection(UUID id) {
        ConnectorEntity entity = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Connector not found: " + id));
        String decryptedCredentials = entity.getCredentialsEncrypted() != null
                ? encryptionService.decrypt(entity.getCredentialsEncrypted())
                : null;
        return switch (entity.getType()) {
            case JDBC -> jdbcConnector.test(entity.getConfig(), decryptedCredentials);
            case HTTP -> httpConnector.test(entity.getConfig());
            case REDIS -> redisConnector.test(entity.getConfig());
        };
    }

    private ConnectorDto toDto(ConnectorEntity entity) {
        return new ConnectorDto(
                entity.getId(), entity.getSpaceId(), entity.getName(), entity.getType(),
                entity.getConfig(), entity.isEnabled(), entity.getDescription(),
                entity.getCreatedBy(), entity.getCreatedAt(), entity.getUpdatedAt()
        );
    }
}
