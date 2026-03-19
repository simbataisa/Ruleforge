package io.ruleforge.event.service;

import io.ruleforge.event.entity.WebhookEntity;
import io.ruleforge.event.model.WebhookAuthConfig;
import io.ruleforge.event.model.WebhookAuthType;
import io.ruleforge.event.repository.WebhookRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class WebhookCredentialMigrationService implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(WebhookCredentialMigrationService.class);

    private final WebhookRepository webhookRepository;
    private final CredentialEncryptionService encryptionService;

    public WebhookCredentialMigrationService(WebhookRepository webhookRepository,
                                             CredentialEncryptionService encryptionService) {
        this.webhookRepository = webhookRepository;
        this.encryptionService = encryptionService;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        List<WebhookEntity> toMigrate = webhookRepository
                .findByAuthTypeAndAuthConfigIsNullAndSecretIsNotNull(WebhookAuthType.SIGNING_SECRET);

        if (toMigrate.isEmpty()) {
            log.info("Webhook credential migration: no rows to migrate");
            return;
        }

        int count = 0;
        for (WebhookEntity webhook : toMigrate) {
            WebhookAuthConfig config = new WebhookAuthConfig.SigningSecret(webhook.getSecret());
            webhook.setAuthConfig(encryptionService.encrypt(config));
            count++;
        }
        webhookRepository.saveAll(toMigrate);
        log.info("Webhook credential migration: encrypted {} signing secrets into auth_config", count);
    }
}
