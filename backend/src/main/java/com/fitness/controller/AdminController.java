package com.fitness.controller;

import com.fitness.common.BusinessException;
import com.fitness.common.PageResult;
import com.fitness.common.Result;
import com.fitness.dto.AdminRequests;
import com.fitness.dto.AdminViews;
import com.fitness.dto.ExerciseVO;
import com.fitness.entity.AdminAuditLog;
import com.fitness.security.LoginUser;
import com.fitness.security.SecurityUtils;
import com.fitness.service.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/users")
    public Result<PageResult<AdminViews.User>> users(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size) {
        requireAdmin();
        return Result.ok(adminService.users(keyword, page, size));
    }

    @PutMapping("/users/{userId}/status")
    public Result<Void> updateUserStatus(@PathVariable Long userId,
                                         @Valid @RequestBody AdminRequests.UserStatus request) {
        LoginUser admin = requireAdmin();
        adminService.updateUserStatus(admin.id(), userId, request);
        return Result.ok();
    }

    @GetMapping("/platform")
    public Result<AdminViews.Platform> platform() {
        requireAdmin();
        return Result.ok(adminService.platform());
    }

    @GetMapping("/exercises")
    public Result<PageResult<ExerciseVO>> exercises(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String bodyPart,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size) {
        requireAdmin();
        return Result.ok(adminService.exercises(keyword, bodyPart, status, page, size));
    }

    @PostMapping("/exercises")
    public Result<ExerciseVO> createExercise(@Valid @RequestBody AdminRequests.ExerciseSave request) {
        LoginUser admin = requireAdmin();
        return Result.ok(adminService.createExercise(admin.id(), request));
    }

    @PutMapping("/exercises/{exerciseId}")
    public Result<ExerciseVO> updateExercise(@PathVariable Long exerciseId,
                                             @Valid @RequestBody AdminRequests.ExerciseSave request) {
        LoginUser admin = requireAdmin();
        return Result.ok(adminService.updateExercise(admin.id(), exerciseId, request));
    }

    @PutMapping("/exercises/{exerciseId}/status")
    public Result<Void> changeExerciseStatus(@PathVariable Long exerciseId,
                                             @RequestParam String status) {
        LoginUser admin = requireAdmin();
        adminService.changeExerciseStatus(admin.id(), exerciseId, status);
        return Result.ok();
    }

    @GetMapping("/audit-logs")
    public Result<List<AdminAuditLog>> auditLogs(@RequestParam(defaultValue = "50") int limit) {
        requireAdmin();
        return Result.ok(adminService.auditLogs(limit));
    }

    private LoginUser requireAdmin() {
        LoginUser current = SecurityUtils.current();
        if (!"ADMIN".equals(current.role())) {
            throw new BusinessException(403, "需要管理员权限");
        }
        return current;
    }
}
