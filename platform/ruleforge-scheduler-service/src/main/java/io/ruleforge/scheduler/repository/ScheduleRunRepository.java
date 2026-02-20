package io.ruleforge.scheduler.repository;

import io.ruleforge.scheduler.entity.ScheduleRunEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ScheduleRunRepository extends JpaRepository<ScheduleRunEntity, UUID> {

    List<ScheduleRunEntity> findByScheduleIdOrderByStartedAtDesc(UUID scheduleId);

    Page<ScheduleRunEntity> findByScheduleId(UUID scheduleId, Pageable pageable);
}
