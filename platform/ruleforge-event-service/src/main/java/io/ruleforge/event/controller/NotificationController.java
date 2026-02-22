package io.ruleforge.event.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.ruleforge.common.tenant.TenantContext;
import io.ruleforge.event.entity.AuditEventEntity;
import io.ruleforge.event.repository.AuditEventRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Instant;
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
    private final ObjectMapper objectMapper;

    // orgId -> list of active emitters
    private final ConcurrentHashMap<UUID, CopyOnWriteArrayList<SseEmitter>> emitters = new ConcurrentHashMap<>();
    private volatile Instant lastPollTime = Instant.now();

    public NotificationController(AuditEventRepository auditEventRepository, ObjectMapper objectMapper) {
        this.auditEventRepository = auditEventRepository;
        this.objectMapper = objectMapper;
    }

    @GetMapping(value = "/stream", produces = "text/event-stream")
    public SseEmitter stream() {
        UUID orgId = TenantContext.get().orgId();
        SseEmitter emitter = new SseEmitter(EMITTER_TIMEOUT);

        CopyOnWriteArrayList<SseEmitter> orgEmitters = emitters.computeIfAbsent(orgId, k -> new CopyOnWriteArrayList<>());
        orgEmitters.add(emitter);

        Runnable removeEmitter = () -> {
            orgEmitters.remove(emitter);
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

        for (Map.Entry<UUID, CopyOnWriteArrayList<SseEmitter>> entry : emitters.entrySet()) {
            UUID orgId = entry.getKey();
            CopyOnWriteArrayList<SseEmitter> orgEmitters = entry.getValue();

            if (orgEmitters.isEmpty()) {
                continue;
            }

            try {
                List<AuditEventEntity> newEvents = auditEventRepository
                        .findByOrgIdAndTimestampAfterOrderByTimestampDesc(orgId, pollSince);

                for (AuditEventEntity event : newEvents) {
                    String json = objectMapper.writeValueAsString(Map.of(
                            "id", event.getId().toString(),
                            "eventType", event.getEventType(),
                            "resourceType", event.getResourceType() != null ? event.getResourceType() : "",
                            "resourceId", event.getResourceId() != null ? event.getResourceId() : "",
                            "timestamp", event.getTimestamp().toString()
                    ));

                    List<SseEmitter> deadEmitters = new java.util.ArrayList<>();
                    for (SseEmitter emitter : orgEmitters) {
                        try {
                            emitter.send(SseEmitter.event().name("notification").data(json));
                        } catch (IOException e) {
                            deadEmitters.add(emitter);
                        }
                    }
                    orgEmitters.removeAll(deadEmitters);
                }
            } catch (Exception e) {
                log.warn("Error polling events for org {}: {}", orgId, e.getMessage());
            }
        }
    }
}
