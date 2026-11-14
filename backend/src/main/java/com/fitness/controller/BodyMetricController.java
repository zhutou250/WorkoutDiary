package com.fitness.controller;

import com.fitness.common.Result;
import com.fitness.dto.BodyMetricRequests;
import com.fitness.entity.BodyMetric;
import com.fitness.security.SecurityUtils;
import com.fitness.service.BodyMetricService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/metrics")
@RequiredArgsConstructor
public class BodyMetricController {

    private final BodyMetricService metricService;

    @GetMapping
    public Result<List<BodyMetric>> list(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        return Result.ok(metricService.list(SecurityUtils.currentUserId(), start, end));
    }

    @PostMapping
    public Result<BodyMetric> upsert(@Valid @RequestBody BodyMetricRequests.Upsert request) {
        return Result.ok(metricService.upsert(SecurityUtils.currentUserId(), request));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        metricService.delete(SecurityUtils.currentUserId(), id);
        return Result.ok();
    }
}
