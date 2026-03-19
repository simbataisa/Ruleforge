package io.ruleforge.event.repository;

import java.time.LocalDate;

public interface DailyDeliveryStats {
    LocalDate getDate();
    int getSuccess();
    int getFailed();
}
