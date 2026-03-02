package io.ruleforge.scheduler.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record CreateHolidayEntryRequest(
    @NotNull LocalDate date,
    @NotBlank String name,
    String entryType,
    Boolean recurring
) {}
