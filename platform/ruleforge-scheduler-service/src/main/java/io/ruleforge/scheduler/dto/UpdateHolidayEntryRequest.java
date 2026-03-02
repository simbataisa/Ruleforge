package io.ruleforge.scheduler.dto;

import java.time.LocalDate;

public record UpdateHolidayEntryRequest(LocalDate date, String name, String entryType, Boolean recurring) {}
