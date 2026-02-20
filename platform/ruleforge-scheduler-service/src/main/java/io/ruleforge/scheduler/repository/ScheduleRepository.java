package io.ruleforge.scheduler.repository;

import io.ruleforge.scheduler.entity.ScheduleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ScheduleRepository extends JpaRepository<ScheduleEntity, UUID> {

    List<ScheduleEntity> findBySpaceId(UUID spaceId);

    List<ScheduleEntity> findBySpaceIdAndEnabledTrue(UUID spaceId);

    Optional<ScheduleEntity> findByIdAndSpaceId(UUID id, UUID spaceId);
}
