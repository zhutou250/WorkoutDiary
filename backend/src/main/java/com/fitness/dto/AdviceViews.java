package com.fitness.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class AdviceViews {

    private AdviceViews() {
    }

    public record Summary(
            Long id,
            LocalDate periodStart,
            LocalDate periodEnd,
            String status,
            String modelName,
            boolean fallbackUsed,
            LocalDateTime createdAt,
            int itemCount
    ) {
    }

    public record Detail(
            Long id,
            LocalDate periodStart,
            LocalDate periodEnd,
            String status,
            String modelName,
            boolean fallbackUsed,
            LocalDateTime createdAt,
            List<Item> items
    ) {
    }

    public record Item(
            String findingKey,
            String ruleCode,
            String severity,
            String title,
            Object evidence,
            String suggestionText,
            String source
    ) {
    }
}
