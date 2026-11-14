package com.fitness.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public final class SessionRequests {

    private SessionRequests() {
    }

    public record Start(
            @NotNull(message = "请选择训练日")
            Long planDayId
    ) {
    }

    public record SetUpsert(
            @Min(value = 1, message = "组号从 1 开始")
            Integer setNo,

            String phase,

            String setType,

            String side,

            @DecimalMin(value = "0", message = "重量不能为负数")
            BigDecimal weightKg,

            @Min(value = 0, message = "次数不能为负数")
            Integer reps,

            @Min(value = 0, message = "时长不能为负数")
            Integer durationSec,

            @DecimalMin(value = "0", message = "距离不能为负数")
            BigDecimal distanceM,

            @Min(value = 0, message = "RIR 不能小于 0")
            @Max(value = 10, message = "RIR 不能大于 10")
            Integer rir,

            @DecimalMin(value = "1", message = "RPE 不能小于 1")
            @DecimalMax(value = "10", message = "RPE 不能大于 10")
            BigDecimal rpe,

            Boolean isFailure,

            Boolean isCompleted,

            @Size(max = 255, message = "本组备注不能超过 255 个字符")
            String note
    ) {
    }

    public record Finish(
            @DecimalMin(value = "1", message = "整体 RPE 不能小于 1")
            @DecimalMax(value = "10", message = "整体 RPE 不能大于 10")
            BigDecimal overallRpe,

            String feeling,

            @Size(max = 500, message = "训练备注不能超过 500 个字符")
            String note
    ) {
    }

    public record AddExercise(
            @NotNull(message = "请选择动作")
            Long exerciseId,

            @Size(max = 255, message = "动作备注不能超过 255 个字符")
            String actualNote
    ) {
    }

    public record ReplaceExercise(
            @NotNull(message = "请选择替换动作")
            Long exerciseId
    ) {
    }
}
