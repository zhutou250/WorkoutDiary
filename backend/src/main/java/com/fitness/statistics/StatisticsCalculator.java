package com.fitness.statistics;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class StatisticsCalculator {

    private StatisticsCalculator() {
    }

    public static BigDecimal volume(BigDecimal weightKg, Integer reps) {
        if (weightKg == null || reps == null) {
            return BigDecimal.ZERO;
        }
        return weightKg.multiply(BigDecimal.valueOf(reps)).setScale(2, RoundingMode.HALF_UP);
    }

    public static BigDecimal epley1Rm(BigDecimal weightKg, int reps) {
        if (weightKg == null || weightKg.compareTo(BigDecimal.ZERO) <= 0 || reps < 1 || reps > 10) {
            return null;
        }
        BigDecimal factor = BigDecimal.ONE.add(
                BigDecimal.valueOf(reps).divide(BigDecimal.valueOf(30), 6, RoundingMode.HALF_UP)
        );
        return weightKg.multiply(factor).setScale(2, RoundingMode.HALF_UP);
    }

    public static BigDecimal sideFactor(String side) {
        return "L".equals(side) || "R".equals(side)
                ? new BigDecimal("0.50")
                : BigDecimal.ONE;
    }

    public static BigDecimal completionPct(int completedSets, int plannedSets) {
        if (plannedSets <= 0) {
            return null;
        }
        return BigDecimal.valueOf(completedSets)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(plannedSets), 1, RoundingMode.HALF_UP);
    }
}
