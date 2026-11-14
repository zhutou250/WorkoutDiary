package com.fitness.controller;

import com.fitness.common.PageResult;
import com.fitness.common.Result;
import com.fitness.dto.ExerciseVO;
import com.fitness.service.ExerciseService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/exercises")
@RequiredArgsConstructor
public class ExerciseController {

    private final ExerciseService exerciseService;

    @GetMapping
    public Result<PageResult<ExerciseVO>> page(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String bodyPart,
            @RequestParam(required = false) Long equipmentId,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size) {
        return Result.ok(exerciseService.page(keyword, bodyPart, equipmentId, page, size));
    }

    @GetMapping("/meta")
    public Result<Map<String, Object>> meta() {
        return Result.ok(exerciseService.meta());
    }

    @GetMapping("/{id}")
    public Result<ExerciseVO> detail(@PathVariable Long id) {
        return Result.ok(exerciseService.getById(id));
    }
}
