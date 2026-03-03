package io.ruleforge.scheduler.service;

import io.ruleforge.scheduler.dto.*;
import io.ruleforge.scheduler.entity.ScheduleEntity;
import io.ruleforge.scheduler.entity.ScheduleRunEntity;
import io.ruleforge.scheduler.entity.RunStatus;
import io.ruleforge.scheduler.repository.HolidayCalendarRepository;
import io.ruleforge.scheduler.repository.ScheduleRepository;
import io.ruleforge.scheduler.repository.ScheduleRunRepository;
import io.ruleforge.scheduler.workflow.repository.WorkflowRepository;
import org.quartz.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class ScheduleService {

    private final ScheduleRepository scheduleRepository;
    private final ScheduleRunRepository scheduleRunRepository;
    private final WorkflowRepository workflowRepository;
    private final HolidayCalendarRepository holidayCalendarRepository;
    private final Scheduler quartzScheduler;

    public ScheduleService(ScheduleRepository scheduleRepository,
                            ScheduleRunRepository scheduleRunRepository,
                            WorkflowRepository workflowRepository,
                            HolidayCalendarRepository holidayCalendarRepository,
                            Scheduler quartzScheduler) {
        this.scheduleRepository = scheduleRepository;
        this.scheduleRunRepository = scheduleRunRepository;
        this.workflowRepository = workflowRepository;
        this.holidayCalendarRepository = holidayCalendarRepository;
        this.quartzScheduler = quartzScheduler;
    }

    public ScheduleDto create(CreateScheduleRequest request) {
        if (request.ruleId() == null && request.workflowId() == null) {
            throw new IllegalArgumentException("Schedule must have either ruleId or workflowId");
        }

        ScheduleEntity entity = new ScheduleEntity();
        entity.setSpaceId(request.spaceId());
        entity.setRuleId(request.ruleId());
        entity.setWorkflowId(request.workflowId());
        entity.setName(request.name());
        entity.setCronExpression(request.cronExpression());
        entity.setInputTemplate(request.inputTemplate() != null ? request.inputTemplate() : java.util.Map.of());
        entity.setDescription(request.description());
        entity.setRuleName(request.ruleName());
        entity.setRuleType(request.ruleType());
        entity.setCreatedBy(request.createdBy());
        if (request.holidayCalendarId() != null) entity.setHolidayCalendarId(request.holidayCalendarId());
        if (request.skipOnHoliday() != null) entity.setSkipOnHoliday(request.skipOnHoliday());
        ScheduleEntity saved = scheduleRepository.save(entity);

        if (saved.getWorkflowId() != null) {
            syncWorkflowTrigger(saved);
        }

        if (saved.isEnabled()) {
            scheduleQuartzJob(saved);
        }

        return toDto(saved);
    }

    @Transactional(readOnly = true)
    public ScheduleDto findById(UUID id) {
        return scheduleRepository.findById(id)
                .map(this::toDto)
                .orElseThrow(() -> new RuntimeException("Schedule not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<ScheduleDto> findBySpaceId(UUID spaceId) {
        return scheduleRepository.findBySpaceId(spaceId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ScheduleDto> findAll() {
        return scheduleRepository.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public ScheduleDto update(UUID id, UpdateScheduleRequest request) {
        ScheduleEntity entity = scheduleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Schedule not found: " + id));

        if (request.name() != null) entity.setName(request.name());
        if (request.cronExpression() != null) entity.setCronExpression(request.cronExpression());
        if (request.inputTemplate() != null) entity.setInputTemplate(request.inputTemplate());
        if (request.description() != null) entity.setDescription(request.description());
        if (request.workflowId() != null) entity.setWorkflowId(request.workflowId());
        if (request.holidayCalendarId() != null) entity.setHolidayCalendarId(request.holidayCalendarId());
        if (request.skipOnHoliday() != null) entity.setSkipOnHoliday(request.skipOnHoliday());

        if (request.enabled() != null) {
            boolean wasEnabled = entity.isEnabled();
            entity.setEnabled(request.enabled());
            if (!wasEnabled && request.enabled()) {
                scheduleQuartzJob(entity);
            } else if (wasEnabled && !request.enabled()) {
                unscheduleQuartzJob(entity.getId());
            }
        }

        ScheduleEntity updated = scheduleRepository.save(entity);

        if (updated.getWorkflowId() != null) {
            syncWorkflowTrigger(updated);
        }

        return toDto(updated);
    }

    public void delete(UUID id) {
        unscheduleQuartzJob(id);
        scheduleRepository.deleteById(id);
    }

    public ScheduleRunDto triggerManually(UUID id) {
        ScheduleEntity entity = scheduleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Schedule not found: " + id));

        ScheduleRunEntity run = new ScheduleRunEntity();
        run.setScheduleId(id);
        run.setStatus(RunStatus.RUNNING);
        run.setStartedAt(Instant.now());
        run = scheduleRunRepository.save(run);

        try {
            quartzScheduler.triggerJob(JobKey.jobKey("schedule-" + id));
            run.setStatus(RunStatus.SUCCESS);
        } catch (Exception e) {
            run.setStatus(RunStatus.FAILED);
            run.setErrorMessage(e.getMessage());
        } finally {
            run.setCompletedAt(Instant.now());
            scheduleRunRepository.save(run);
        }

        return toRunDto(run);
    }

    @Transactional(readOnly = true)
    public Page<ScheduleRunDto> getRuns(UUID scheduleId, Pageable pageable) {
        return scheduleRunRepository.findByScheduleId(scheduleId, pageable).map(this::toRunDto);
    }

    @SuppressWarnings("unchecked")
    private void syncWorkflowTrigger(ScheduleEntity schedule) {
        workflowRepository.findById(schedule.getWorkflowId()).ifPresent(workflow -> {
            workflow.setTriggerType("CRON");
            workflow.setCronExpression(schedule.getCronExpression());
            Map<String, Object> config = workflow.getTriggerConfig() != null
                    ? new HashMap<>(workflow.getTriggerConfig()) : new HashMap<>();
            config.put("triggerType", "CRON");
            config.put("cronExpression", schedule.getCronExpression());
            config.put("scheduleId", schedule.getId().toString());
            if (schedule.getHolidayCalendarId() != null) {
                config.put("holidayCalendarId", schedule.getHolidayCalendarId().toString());
                config.put("skipOnHoliday", schedule.getSkipOnHoliday());
            }
            workflow.setTriggerConfig(config);
            if (schedule.getHolidayCalendarId() != null) {
                workflow.setHolidayCalendarId(schedule.getHolidayCalendarId());
                workflow.setSkipOnHoliday(schedule.getSkipOnHoliday());
            }
            // Update the workflow definition to set trigger node config
            Map<String, Object> definition = workflow.getDefinition();
            if (definition != null && definition.containsKey("nodes")) {
                List<Map<String, Object>> nodes = (List<Map<String, Object>>) definition.get("nodes");
                for (Map<String, Object> node : nodes) {
                    if ("trigger".equals(node.get("type"))) {
                        Map<String, Object> data = (Map<String, Object>) node.get("data");
                        if (data == null) {
                            data = new HashMap<>();
                            node.put("data", data);
                        }
                        Map<String, Object> nodeConfig = new HashMap<>();
                        nodeConfig.put("triggerType", "CRON");
                        nodeConfig.put("cronExpression", schedule.getCronExpression());
                        if (schedule.getHolidayCalendarId() != null) {
                            nodeConfig.put("holidayCalendarId", schedule.getHolidayCalendarId().toString());
                            nodeConfig.put("skipOnHoliday", schedule.getSkipOnHoliday());
                        }
                        data.put("config", nodeConfig);
                        data.put("label", "Cron: " + schedule.getCronExpression());
                        break;
                    }
                }
                workflow.setDefinition(definition);
            }
            workflowRepository.save(workflow);
        });
    }

    private void scheduleQuartzJob(ScheduleEntity entity) {
        try {
            JobDetail job = JobBuilder.newJob(ScheduleExecutorJob.class)
                    .withIdentity("schedule-" + entity.getId())
                    .usingJobData("scheduleId", entity.getId().toString())
                    .storeDurably()
                    .build();

            CronTrigger trigger = TriggerBuilder.newTrigger()
                    .withIdentity("trigger-" + entity.getId())
                    .withSchedule(CronScheduleBuilder.cronSchedule(entity.getCronExpression()))
                    .build();

            if (quartzScheduler.checkExists(job.getKey())) {
                quartzScheduler.rescheduleJob(trigger.getKey(), trigger);
            } else {
                quartzScheduler.scheduleJob(job, trigger);
            }
        } catch (Exception e) {
            // Log but don't fail - schedule saved, Quartz will retry
        }
    }

    private void unscheduleQuartzJob(UUID scheduleId) {
        try {
            quartzScheduler.deleteJob(JobKey.jobKey("schedule-" + scheduleId));
        } catch (Exception e) {
            // Ignore - job may not have been scheduled
        }
    }

    @Transactional(readOnly = true)
    public ScheduleStatsDto getStats() {
        long total = scheduleRepository.count();
        long active = scheduleRepository.countByEnabledTrue();
        long paused = total - active;
        Instant since = Instant.now().minus(24, java.time.temporal.ChronoUnit.HOURS);
        long successLast24h = scheduleRunRepository.countByStatusAndStartedAtAfter(RunStatus.SUCCESS, since);
        long failedLast24h = scheduleRunRepository.countByStatusAndStartedAtAfter(RunStatus.FAILED, since);
        return new ScheduleStatsDto(total, active, paused, successLast24h, failedLast24h);
    }

    public ScheduleDto pause(UUID id) {
        ScheduleEntity entity = scheduleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Schedule not found: " + id));
        entity.setEnabled(false);
        unscheduleQuartzJob(id);
        return toDto(scheduleRepository.save(entity));
    }

    public ScheduleDto resume(UUID id) {
        ScheduleEntity entity = scheduleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Schedule not found: " + id));
        entity.setEnabled(true);
        scheduleQuartzJob(entity);
        return toDto(scheduleRepository.save(entity));
    }

    private ScheduleDto toDto(ScheduleEntity e) {
        String status = e.isEnabled() ? "ACTIVE" : "PAUSED";

        Instant nextRunAt = null;
        try {
            var trigger = quartzScheduler.getTrigger(TriggerKey.triggerKey("trigger-" + e.getId()));
            if (trigger != null && trigger.getNextFireTime() != null) {
                nextRunAt = trigger.getNextFireTime().toInstant();
            }
        } catch (Exception ignored) {
        }

        Instant lastRunAt = scheduleRunRepository.findTopByScheduleIdOrderByStartedAtDesc(e.getId())
                .map(ScheduleRunEntity::getStartedAt)
                .orElse(null);

        String workflowName = null;
        if (e.getWorkflowId() != null) {
            workflowName = workflowRepository.findById(e.getWorkflowId())
                    .map(w -> w.getName())
                    .orElse(null);
        }

        String holidayCalendarName = null;
        if (e.getHolidayCalendarId() != null) {
            holidayCalendarName = holidayCalendarRepository.findById(e.getHolidayCalendarId())
                    .map(c -> c.getName())
                    .orElse(null);
        }

        return new ScheduleDto(e.getId(), e.getSpaceId(), e.getRuleId(), e.getName(),
                e.getCronExpression(), e.getInputTemplate(), e.isEnabled(), e.getDescription(),
                e.getRuleName(), e.getRuleType(),
                e.getCreatedBy(), e.getCreatedAt(), e.getUpdatedAt(),
                status, nextRunAt, lastRunAt,
                e.getWorkflowId(), workflowName,
                e.getHolidayCalendarId(), e.getSkipOnHoliday(), holidayCalendarName,
                e.getWorkflowId() != null);
    }

    private ScheduleRunDto toRunDto(ScheduleRunEntity e) {
        return new ScheduleRunDto(e.getId(), e.getScheduleId(), e.getStatus(),
                e.getStartedAt(), e.getCompletedAt(), e.getResultLocation(),
                e.getErrorMessage(), e.getRecordsProcessed(), e.getCreatedAt());
    }
}
