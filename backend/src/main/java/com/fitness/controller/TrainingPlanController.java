package com.fitness.controller;

import com.fitness.common.Result;
import com.fitness.dto.PlanRequests;
import com.fitness.dto.PlanViews;
import com.fitness.security.SecurityUtils;
import com.fitness.service.TrainingPlanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/plans")
@RequiredArgsConstructor
public class TrainingPlanController {

    private final TrainingPlanService planService;

    @GetMapping
    public Result<List<PlanViews.Summary>> list() {
        return Result.ok(planService.list(SecurityUtils.currentUserId()));
    }

    @GetMapping("/templates")
    public Result<List<PlanViews.Summary>> templates() {
        return Result.ok(planService.templates());
    }

    @PostMapping("/{templateId}/copy")
    public Result<PlanViews.Detail> copyTemplate(@PathVariable Long templateId) {
        return Result.ok(planService.copyTemplate(SecurityUtils.currentUserId(), templateId));
    }

    @PostMapping
    public Result<PlanViews.Detail> create(@Valid @RequestBody PlanRequests.Create request) {
        return Result.ok(planService.create(SecurityUtils.currentUserId(), request));
    }

    @GetMapping("/{planId}")
    public Result<PlanViews.Detail> detail(@PathVariable Long planId) {
        return Result.ok(planService.detail(SecurityUtils.currentUserId(), planId));
    }

    @PutMapping("/{planId}")
    public Result<PlanViews.Detail> update(@PathVariable Long planId,
                                           @Valid @RequestBody PlanRequests.Update request) {
        return Result.ok(planService.update(SecurityUtils.currentUserId(), planId, request));
    }

    @DeleteMapping("/{planId}")
    public Result<Void> delete(@PathVariable Long planId) {
        planService.delete(SecurityUtils.currentUserId(), planId);
        return Result.ok();
    }

    @PostMapping("/{planId}/days")
    public Result<PlanViews.Detail> addDay(@PathVariable Long planId,
                                           @Valid @RequestBody PlanRequests.Day request) {
        return Result.ok(planService.addDay(SecurityUtils.currentUserId(), planId, request));
    }

    @PutMapping("/days/{dayId}")
    public Result<PlanViews.Detail> updateDay(@PathVariable Long dayId,
                                              @Valid @RequestBody PlanRequests.Day request) {
        return Result.ok(planService.updateDay(SecurityUtils.currentUserId(), dayId, request));
    }

    @DeleteMapping("/days/{dayId}")
    public Result<PlanViews.Detail> deleteDay(@PathVariable Long dayId) {
        return Result.ok(planService.deleteDay(SecurityUtils.currentUserId(), dayId));
    }

    @PostMapping("/days/{dayId}/exercises")
    public Result<PlanViews.Detail> addExercise(@PathVariable Long dayId,
                                                @Valid @RequestBody PlanRequests.Exercise request) {
        return Result.ok(planService.addExercise(SecurityUtils.currentUserId(), dayId, request));
    }

    @PutMapping("/exercises/{planExerciseId}")
    public Result<PlanViews.Detail> updateExercise(@PathVariable Long planExerciseId,
                                                   @Valid @RequestBody PlanRequests.Exercise request) {
        return Result.ok(planService.updateExercise(SecurityUtils.currentUserId(), planExerciseId, request));
    }

    @DeleteMapping("/exercises/{planExerciseId}")
    public Result<PlanViews.Detail> deleteExercise(@PathVariable Long planExerciseId) {
        return Result.ok(planService.deleteExercise(SecurityUtils.currentUserId(), planExerciseId));
    }
}
