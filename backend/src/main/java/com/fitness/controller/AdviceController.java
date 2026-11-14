package com.fitness.controller;

import com.fitness.common.Result;
import com.fitness.dto.AdminRequests;
import com.fitness.dto.AdviceViews;
import com.fitness.security.SecurityUtils;
import com.fitness.service.AdviceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/advice")
@RequiredArgsConstructor
public class AdviceController {

    private final AdviceService adviceService;

    @GetMapping
    public Result<List<AdviceViews.Summary>> list() {
        return Result.ok(adviceService.list(SecurityUtils.currentUserId()));
    }

    @GetMapping("/{adviceId}")
    public Result<AdviceViews.Detail> detail(@PathVariable Long adviceId) {
        return Result.ok(adviceService.detail(SecurityUtils.currentUserId(), adviceId));
    }

    @PostMapping("/generate")
    public Result<AdviceViews.Detail> generate() {
        return Result.ok(adviceService.generate(SecurityUtils.currentUserId()));
    }

    @PostMapping("/{adviceId}/feedback")
    public Result<Void> feedback(@PathVariable Long adviceId,
                                 @Valid @RequestBody AdminRequests.Feedback request) {
        adviceService.feedback(SecurityUtils.currentUserId(), adviceId, request);
        return Result.ok();
    }
}
