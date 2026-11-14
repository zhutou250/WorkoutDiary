package com.fitness.statistics;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class StatisticsCalculatorTest {

    @Test
    void volumeMultipliesWeightAndReps() {
        assertEquals(new BigDecimal("480.00"),
                StatisticsCalculator.volume(new BigDecimal("60"), 8));
        assertEquals(BigDecimal.ZERO, StatisticsCalculator.volume(null, 8));
    }

    @Test
    void epleyFormulaMatchesManualExample() {
        assertEquals(new BigDecimal("76.00"),
                StatisticsCalculator.epley1Rm(new BigDecimal("60"), 8));
        assertNull(StatisticsCalculator.epley1Rm(new BigDecimal("60"), 11));
        assertNull(StatisticsCalculator.epley1Rm(BigDecimal.ZERO, 8));
    }

    @Test
    void unilateralSetsCountAsHalf() {
        assertEquals(new BigDecimal("0.50"), StatisticsCalculator.sideFactor("L"));
        assertEquals(new BigDecimal("0.50"), StatisticsCalculator.sideFactor("R"));
        assertEquals(BigDecimal.ONE, StatisticsCalculator.sideFactor("BOTH"));
    }

    @Test
    void completionPercentageIsCappedByDivisor() {
        assertEquals(new BigDecimal("100.0"), StatisticsCalculator.completionPct(6, 6));
        assertEquals(new BigDecimal("83.3"), StatisticsCalculator.completionPct(5, 6));
        assertNull(StatisticsCalculator.completionPct(5, 0));
    }
}
