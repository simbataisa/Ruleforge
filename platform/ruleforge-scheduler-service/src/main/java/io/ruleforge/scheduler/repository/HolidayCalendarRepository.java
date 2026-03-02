package io.ruleforge.scheduler.repository;

import io.ruleforge.scheduler.entity.HolidayCalendarEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface HolidayCalendarRepository extends JpaRepository<HolidayCalendarEntity, UUID> {

    List<HolidayCalendarEntity> findBySpaceId(UUID spaceId);

    List<HolidayCalendarEntity> findByMarket(String market);
}
