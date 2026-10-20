package io.ruleforge.event.controller;

import tools.jackson.databind.ObjectMapper;
import io.ruleforge.common.tenant.TenantContext;
import io.ruleforge.event.entity.AuditEventEntity;
import io.ruleforge.event.entity.NotificationEntity;
import io.ruleforge.event.repository.AuditEventRepository;
import io.ruleforge.event.repository.NotificationRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@RestController
@RequestMapping("/v1/events")
public class NotificationController {

    private static final Logger log = LoggerFactory.getLogger(NotificationController.class);
    private static final long EMITTER_TIMEOUT = 30 * 60 * 1000L; // 30 minutes

    private final AuditEventRepository auditEventRepository;
    private final NotificationRepository notificationRepository;
    private final ObjectMapper objectMapper;

    /**
     * Emitter registry keyed by orgId, storing (userId, SseEmitter) pairs
     * for user-level notification filtering.
     */
    private final ConcurrentHashMap<UUID, CopyOnWriteArrayList<UserEmitter>> emitters = new ConcurrentHashMap<>();
    private volatile Instant lastPollTime = Instant.now();

    public NotificationController(AuditEventRepository auditEventRepository,
                                   NotificationRepository notificationRepository,
                                   ObjectMapper objectMapper) {
        this.auditEventRepository = auditEventRepository;
        this.notificationRepository = notificationRepository;
        this.objectMapper = objectMapper;
    }

    @GetMapping(value = "/stream", produces = "text/event-stream")
    public SseEmitter stream() {
        UUID orgId = TenantContext.get().orgId();
        UUID userId = TenantContext.get().userId();
        SseEmitter emitter = new SseEmitter(EMITTER_TIMEOUT);

        UserEmitter userEmitter = new UserEmitter(userId, emitter);
        CopyOnWriteArrayList<UserEmitter> orgEmitters = emitters.computeIfAbsent(orgId, k -> new CopyOnWriteArrayList<>());
        orgEmitters.add(userEmitter);

        Runnable removeEmitter = () -> {
            orgEmitters.remove(userEmitter);
            if (orgEmitters.isEmpty()) {
                emitters.remove(orgId);
            }
        };

        emitter.onCompletion(removeEmitter);
        emitter.onTimeout(removeEmitter);
        emitter.onError(e -> removeEmitter.run());

        // Send a connection event so the client knows the stream is active
        try {
            emitter.send(SseEmitter.event().name("connected").data("{\"status\":\"connected\"}"));
        } catch (IOException e) {
            log.warn("Failed to send initial SSE event for org {}", orgId);
            removeEmitter.run();
        }

        return emitter;
    }

    @Scheduled(fixedDelay = 5000)
    public void pollAndPush() {
        if (emitters.isEmpty()) {
            lastPollTime = Instant.now();
            return;
        }

        Instant pollSince = lastPollTime;
        lastPollTime = Instant.now();

        for (Map.Entry<UUID, CopyOnWriteArrayList<UserEmitter>> entry : emitters.entrySet()) {
            UUID orgId = entry.getKey();
            CopyOnWriteArrayList<UserEmitter> orgEmitters = entry.getValue();

            if (orgEmitters.isEmpty()) {
                continue;
            }

            // Push audit events to all org emitters (existing behavior)
            pushAuditEvents(orgId, orgEmitters, pollSince);

            // Push user-targeted notifications (new behavior)
            pushUserNotifications(orgId, orgEmitters, pollSince);
        }
    }

    private void pushAuditEvents(UUID orgId, CopyOnWriteArrayList<UserEmitter> orgEmitters, Instant pollSince) {
        try {
            List<AuditEventEntity> newEvents = auditEventRepository
                    .findByOrgIdAndCreatedAtAfterOrderByCreatedAtDesc(orgId, pollSince);

            for (AuditEventEntity event : newEvents) {
                String json = objectMapper.writeValueAsString(Map.of(
                        "id", event.getId().toString(),
                        "eventType", event.getEventType(),
                        "resourceType", event.getResourceType() != null ? event.getResourceType() : "",
                        "resourceId", event.getResourceId() != null ? event.getResourceId() : "",
                        "timestamp", event.getCreatedAt().toString()
                ));

                List<UserEmitter> deadEmitters = new ArrayList<>();
                for (UserEmitter ue : orgEmitters) {
                    try {
                        ue.emitter().send(SseEmitter.event().name("notification").data(json));
                    } catch (IOException e) {
                        deadEmitters.add(ue);
                    }
                }
                orgEmitters.removeAll(deadEmitters);
            }
        } catch (Exception e) {
            log.warn("Error polling audit events for org {}: {}", orgId, e.getMessage());
        }
    }

    private void pushUserNotifications(UUID orgId, CopyOnWriteArrayList<UserEmitter> orgEmitters, Instant pollSince) {
        try {
            List<NotificationEntity> notifications = notificationRepository
                    .findByOrgIdAndCreatedAtAfterOrderByCreatedAtDesc(orgId, pollSince);

            for (NotificationEntity notification : notifications) {
                String json = objectMapper.writeValueAsString(Map.of(
                        "id", notification.getId().toString(),
                        "type", notification.getType(),
                        "title", notification.getTitle(),
                        "body", notification.getBody() != null ? notification.getBody() : "",
                        "resourceType", notification.getResourceType() != null ? notification.getResourceType() : "",
                        "resourceId", notification.getResourceId() != null ? notification.getResourceId() : "",
                        "read", notification.isRead(),
                        "createdAt", notification.getCreatedAt().toString()
                ));

                UUID targetUserId = notification.getUserId();

                List<UserEmitter> deadEmitters = new ArrayList<>();
                for (UserEmitter ue : orgEmitters) {
                    // Only send user-notification to the targeted user
                    if (ue.userId().equals(targetUserId)) {
                        try {
                            ue.emitter().send(SseEmitter.event().name("user-notification").data(json));
                        } catch (IOException e) {
                            deadEmitters.add(ue);
                        }
                    }
                }
                orgEmitters.removeAll(deadEmitters);
            }
        } catch (Exception e) {
            log.warn("Error polling notifications for org {}: {}", orgId, e.getMessage());
        }
    }

    /**
     * Pairs a userId with its SseEmitter for user-level notification filtering.
     */
    private record UserEmitter(UUID userId, SseEmitter emitter) {}
}
