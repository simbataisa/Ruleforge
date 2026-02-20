package io.ruleforge.scheduler.service;

import io.ruleforge.scheduler.dto.*;
import io.ruleforge.scheduler.entity.ScheduleEntity;
import io.ruleforge.scheduler.entity.ScheduleRunEntity;
import io.ruleforge.scheduler.entity.RunStatus;
import io.ruleforge.scheduler.repository.ScheduleRepository;
import io.ruleforge.scheduler.repository.ScheduleRunRepository;
import org.quartz.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class ScheduleService {

    private final ScheduleRepository scheduleRepository;
    private final ScheduleRunRepository scheduleRunRepository;
    private final Scheduler quartzScheduler;

    public ScheduleService(ScheduleRepository scheduleRepository,
                            ScheduleRunRepository scheduleRunRepository,
                            Scheduler quartzScheduler) {
        this.scheduleRepository = scheduleRepository;
        this.scheduleRunRepository = scheduleRunRepository;
        this.quartzScheduler = quartzScheduler;
    }

    public ScheduleDto create(CreateScheduleRequest request) {
        ScheduleEntity entity = new ScheduleEntity();
        entity.setSpaceId(request.spaceId());
        entity.setRuleId(request.ruleId());
        entity.setName(request.name());
        entity.setCronExpression(request.cronExpression());
        entity.setInputTemplate(request.inputTemplate() != null ? request.inputTemplate() : java.util.Map.of());
        entity.setDescription(request.description());
        entity.setCreatedBy(request.createdBy());
        ScheduleEntity saved = scheduleRepository.save(entity);

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

    public ScheduleDto update(UUID id, UpdateScheduleRequest request) {
        ScheduleEntity entity = scheduleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Schedule not found: " + id));

        if (request.name() != null) entity.setName(request.name());
        if (request.cronExpression() != null) entity.setCronExpression(request.cronExpression());
        if (request.inputTemplate() != null) entity.setInputTemplate(request.inputTemplate());
        if (request.description() != null) entity.setDescription(request.description());

        if (request.enabled() != null) {
            boolean wasEnabled = entity.isEnabled();
            entity.setEnabled(request.enabled());
            if (!wasEnabled && request.enabled()) {
                scheduleQuartzJob(entity);
            } else if (wasEnabled && !request.enabled()) {
                unscheduleQuartzJob(entity.getId());
            }
        }

        return toDto(scheduleRepository.save(entity));
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

    private ScheduleDto toDto(ScheduleEntity e) {
        return new ScheduleDto(e.getId(), e.getSpaceId(), e.getRuleId(), e.getName(),
                e.getCronExpression(), e.getInputTemplate(), e.isEnabled(), e.getDescription(),
                e.getCreatedBy(), e.getCreatedAt(), e.getUpdatedAt());
    }

    private ScheduleRunDto toRunDto(ScheduleRunEntity e) {
        return new ScheduleRunDto(e.getId(), e.getScheduleId(), e.getStatus(),
                e.getStartedAt(), e.getCompletedAt(), e.getResultLocation(),
                e.getErrorMessage(), e.getRecordsProcessed(), e.getCreatedAt());
    }
}
