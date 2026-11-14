package com.fitness.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class PlanViews {

    private PlanViews() {
    }

    public record Summary(
            Long id,
            String name,
            String goal,
            LocalDate startDate,
            Integer weeks,
            String status,
            int dayCount,
            int exerciseCount,
            LocalDateTime createdAt
    ) {
    }

    public record Detail(
            Long id,
            String name,
            String goal,
            LocalDate startDate,
            Integer weeks,
            String status,
            List<Day> days
    ) {
    }

    public record Day(
            Long id,
            Integer weekNo,
            Integer dayOfWeek,
            String title,
            Integer sortNo,
            List<Exercise> exercises
    ) {
    }

    public record Exercise(
            Long id,
            Long exerciseId,
            String exerciseName,
            String bodyPart,
            String equipmentName,
            String mode,
            Integer sortNo,
            Integer targetSets,
            Integer targetRepsMin,
            Integer targetRepsMax,
            BigDecimal targetWeightKg,
            Integer targetDurationSec,
            Integer restSeconds,
            String progressionType,
            BigDecimal incrementKg,
            Integer incrementReps,
            String note
    ) {
    }
}
