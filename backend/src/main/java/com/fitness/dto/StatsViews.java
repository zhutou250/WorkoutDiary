package com.fitness.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class StatsViews {

    private StatsViews() {
    }

    public record Overview(
            LocalDate periodStart,
            LocalDate periodEnd,
            BigDecimal totalVolumeKg,
            int completedSessions,
            int totalSessions,
            int completedSets,
            int plannedSets,
            BigDecimal completionPct,
            List<Week> weeks,
            List<Muscle> muscles,
            List<ExerciseStat> exercises
    ) {
    }

    public record Week(
            LocalDate weekStart,
            int sessionCount,
            int completedSets,
            int plannedSets,
            BigDecimal volumeKg
    ) {
    }

    public record Muscle(
            String code,
            String name,
            BigDecimal effectiveSets,
            BigDecimal volumeKg
    ) {
    }

    public record ExerciseStat(
            Long exerciseId,
            String exerciseName,
            BigDecimal volumeKg,
            BigDecimal bestE1rmKg
    ) {
    }

    public record ExerciseTrend(
            Long exerciseId,
            String exerciseName,
            List<Point> points,
            Point pr
    ) {
    }

    public record Point(
            LocalDate date,
            BigDecimal e1rmKg,
            BigDecimal volumeKg
    ) {
    }

    public record WeightTrend(
            List<WeightPoint> points
    ) {
    }

    public record WeightPoint(
            LocalDate date,
            BigDecimal weightKg,
            BigDecimal movingAvg7
    ) {
    }

    public record Calendar(
            List<Day> days
    ) {
    }

    public record Day(
            LocalDate date,
            boolean trained,
            int completedSets,
            BigDecimal volumeKg
    ) {
    }

    public record MuscleWeek(
            LocalDate weekStart,
            List<Muscle> muscles
    ) {
    }
}
