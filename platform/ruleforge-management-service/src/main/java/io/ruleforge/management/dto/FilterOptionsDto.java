package io.ruleforge.management.dto;

import java.util.List;

public record FilterOptionsDto(
        List<String> statuses,
        List<String> types,
        List<SpaceOption> spaces
) {
    public record SpaceOption(String id, String name) {}
}
