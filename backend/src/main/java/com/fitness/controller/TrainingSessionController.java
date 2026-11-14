package com.fitness.controller;

import com.fitness.common.PageResult;
import com.fitness.common.Result;
import com.fitness.dto.SessionRequests;
import com.fitness.dto.SessionViews;
import com.fitness.security.SecurityUtils;
import com.fitness.service.TrainingSessionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/sessions")
@RequiredArgsConstructor
public class TrainingSessionController {

    private final TrainingSessionService sessionService;

    @GetMapping
    public Result<PageResult<SessionViews.Summary>> list(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size) {
        return Result.ok(sessionService.list(SecurityUtils.currentUserId(), status, page, size));
    }

    @GetMapping("/active")
    public Result<SessionViews.Detail> active() {
        return Result.ok(sessionService.active(SecurityUtils.currentUserId()));
    }

    @PostMapping("/start")
    public Result<SessionViews.Detail> start(@Valid @RequestBody SessionRequests.Start request) {
        return Result.ok(sessionService.start(SecurityUtils.currentUserId(), request.planDayId()));
    }

    @PostMapping("/free")
    public Result<SessionViews.Detail> startFree() {
        return Result.ok(sessionService.startFree(SecurityUtils.currentUserId()));
    }

    @GetMapping("/{sessionId}")
    public Result<SessionViews.Detail> detail(@PathVariable Long sessionId) {
        return Result.ok(sessionService.detail(SecurityUtils.currentUserId(), sessionId));
    }

    @PostMapping("/{sessionId}/finish")
    public Result<SessionViews.Detail> finish(@PathVariable Long sessionId,
                                              @Valid @RequestBody SessionRequests.Finish request) {
        return Result.ok(sessionService.finish(SecurityUtils.currentUserId(), sessionId, request));
    }

    @PostMapping("/{sessionId}/cancel")
    public Result<SessionViews.Detail> cancel(@PathVariable Long sessionId) {
        return Result.ok(sessionService.cancel(SecurityUtils.currentUserId(), sessionId));
    }

    @PostMapping("/{sessionId}/exercises")
    public Result<SessionViews.Detail> addExercise(@PathVariable Long sessionId,
                                                   @Valid @RequestBody SessionRequests.AddExercise request) {
        return Result.ok(sessionService.addExercise(SecurityUtils.currentUserId(), sessionId, request));
    }

    @PostMapping("/{sessionId}/exercises/{sessionExerciseId}/replace")
    public Result<SessionViews.Detail> replaceExercise(
            @PathVariable Long sessionId,
            @PathVariable Long sessionExerciseId,
            @Valid @RequestBody SessionRequests.ReplaceExercise request) {
        return Result.ok(sessionService.replaceExercise(
                SecurityUtils.currentUserId(), sessionId, sessionExerciseId, request
        ));
    }

    @PostMapping("/{sessionId}/exercises/{sessionExerciseId}/skip")
    public Result<SessionViews.Detail> skipExercise(@PathVariable Long sessionId,
                                                    @PathVariable Long sessionExerciseId) {
        return Result.ok(sessionService.skipExercise(
                SecurityUtils.currentUserId(), sessionId, sessionExerciseId
        ));
    }

    @PostMapping("/{sessionId}/exercises/{sessionExerciseId}/sets")
    public Result<SessionViews.SetItem> addSet(
            @PathVariable Long sessionId,
            @PathVariable Long sessionExerciseId,
            @Valid @RequestBody SessionRequests.SetUpsert request) {
        return Result.ok(sessionService.addSet(
                SecurityUtils.currentUserId(), sessionId, sessionExerciseId, request
        ));
    }

    @PutMapping("/sets/{setId}")
    public Result<SessionViews.SetItem> updateSet(@PathVariable Long setId,
                                                  @Valid @RequestBody SessionRequests.SetUpsert request) {
        return Result.ok(sessionService.updateSet(SecurityUtils.currentUserId(), setId, request));
    }

    @DeleteMapping("/sets/{setId}")
    public Result<Void> deleteSet(@PathVariable Long setId) {
        sessionService.deleteSet(SecurityUtils.currentUserId(), setId);
        return Result.ok();
    }
}
