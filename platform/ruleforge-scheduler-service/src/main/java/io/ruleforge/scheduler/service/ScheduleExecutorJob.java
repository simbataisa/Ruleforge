package io.ruleforge.scheduler.service;

import io.ruleforge.scheduler.entity.RunStatus;
import io.ruleforge.scheduler.entity.ScheduleEntity;
import io.ruleforge.scheduler.entity.ScheduleRunEntity;
import io.ruleforge.scheduler.repository.ScheduleRepository;
import io.ruleforge.scheduler.repository.ScheduleRunRepository;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
public class ScheduleExecutorJob implements Job {

    @Autowired
    private ScheduleRepository scheduleRepository;

    @Autowired
    private ScheduleRunRepository scheduleRunRepository;

    @Autowired
    private SolverClient solverClient;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        String scheduleIdStr = context.getJobDetail().getJobDataMap().getString("scheduleId");
        UUID scheduleId = UUID.fromString(scheduleIdStr);

        ScheduleRunEntity run = new ScheduleRunEntity();
        run.setScheduleId(scheduleId);
        run.setStatus(RunStatus.RUNNING);
        run.setStartedAt(Instant.now());
        run = scheduleRunRepository.save(run);

        try {
            ScheduleEntity schedule = scheduleRepository.findById(scheduleId)
                    .orElseThrow(() -> new RuntimeException("Schedule not found: " + scheduleId));

            solverClient.solve(schedule.getRuleId(), schedule.getInputTemplate());

            run.setStatus(RunStatus.SUCCESS);
            run.setRecordsProcessed(1);
            run.setResultLocation("inline");
        } catch (Exception e) {
            run.setStatus(RunStatus.FAILED);
            run.setErrorMessage(e.getMessage());
        } finally {
            run.setCompletedAt(Instant.now());
            scheduleRunRepository.save(run);
        }
    }
}
