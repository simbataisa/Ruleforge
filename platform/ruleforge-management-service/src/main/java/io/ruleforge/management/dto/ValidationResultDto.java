package io.ruleforge.management.dto;

import java.util.List;

public record ValidationResultDto(
        boolean valid,
        List<ErrorDto> errors,
        List<WarningDto> warnings
) {

    public record ErrorDto(
            String field,
            String message,
            String code
    ) {}

    public record WarningDto(
            String field,
            String message,
            String code
    ) {}
}
