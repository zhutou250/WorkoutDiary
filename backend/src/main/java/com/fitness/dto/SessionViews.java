package com.fitness.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class SessionViews {

    private SessionViews() {
    }

    public record Summary(
            Long id,
            LocalDate sessionDate,
            String planNameSnapshot,
            String planDayTitleSnapshot,
            String status,
            LocalDateTime startedAt,
            LocalDateTime endedAt,
            int exerciseCount,
            int completedSets,
            int totalSets,
            BigDecimal volumeKg
    ) {
    }

    public record Detail(
            Long id,
            Long planDayId,
            String planNameSnapshot,
            String planDayTitleSnapshot,
            LocalDate sessionDate,
            LocalDateTime startedAt,
            LocalDateTime endedAt,
            String status,
            BigDecimal overallRpe,
            String feeling,
            String note,
            int exerciseCount,
            int completedSets,
            int totalSets,
            BigDecimal volumeKg,
            List<Exercise> exercises
    ) {
    }

    public record Exercise(
            Long id,
            Long exerciseId,
            String exerciseName,
            String bodyPart,
            String mode,
            Integer sortNo,
            Integer plannedSets,
            Integer plannedRepsMin,
            Integer plannedRepsMax,
            BigDecimal plannedWeightKg,
            Integer plannedDurationSec,
            String deviationType,
            String actualNote,
            int completedSets,
            BigDecimal volumeKg,
            BigDecimal estimated1RmKg,
            List<SetItem> sets
    ) {
    }

    public record SetItem(
            Long id,
            Integer setNo,
            String phase,
            String setType,
            String side,
            BigDecimal weightKg,
            Integer reps,
            Integer durationSec,
            BigDecimal distanceM,
            Integer rir,
            BigDecimal rpe,
            Boolean isFailure,
            Boolean isCompleted,
            LocalDateTime completedAt,
            String note
    ) {
    }
}
