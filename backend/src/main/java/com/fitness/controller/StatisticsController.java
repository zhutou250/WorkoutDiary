package com.fitness.controller;

import com.fitness.common.Result;
import com.fitness.dto.StatsViews;
import com.fitness.security.SecurityUtils;
import com.fitness.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/stats")
@RequiredArgsConstructor
public class StatisticsController {

    private final StatisticsService statisticsService;

    @GetMapping("/overview")
    public Result<StatsViews.Overview> overview(@RequestParam(defaultValue = "12") int weeks) {
        return Result.ok(statisticsService.overview(SecurityUtils.currentUserId(), weeks));
    }

    @GetMapping("/exercises/{exerciseId}/trend")
    public Result<StatsViews.ExerciseTrend> exerciseTrend(@PathVariable Long exerciseId) {
        return Result.ok(statisticsService.exerciseTrend(SecurityUtils.currentUserId(), exerciseId));
    }

    @GetMapping("/weight")
    public Result<StatsViews.WeightTrend> weightTrend() {
        return Result.ok(statisticsService.weightTrend(SecurityUtils.currentUserId()));
    }

    @GetMapping("/calendar")
    public Result<StatsViews.Calendar> calendar(
            @RequestParam int year,
            @RequestParam int month) {
        return Result.ok(statisticsService.calendar(SecurityUtils.currentUserId(), year, month));
    }
}
