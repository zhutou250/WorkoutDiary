package com.fitness.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public final class PlanRequests {

    private PlanRequests() {
    }

    public record Create(
            @NotBlank(message = "请输入计划名称")
            @Size(max = 100, message = "计划名称不能超过 100 个字符")
            String name,

            @Size(max = 30, message = "训练目标取值不正确")
            String goal,

            LocalDate startDate,

            @Min(value = 1, message = "计划周数至少为 1")
            @Max(value = 52, message = "计划周数不能超过 52")
            Integer weeks
    ) {
    }

    public record Update(
            @Size(min = 1, max = 100, message = "计划名称长度需为 1—100 个字符")
            String name,

            @Size(max = 30, message = "训练目标取值不正确")
            String goal,

            LocalDate startDate,

            @Min(value = 1, message = "计划周数至少为 1")
            @Max(value = 52, message = "计划周数不能超过 52")
            Integer weeks,

            @Pattern(regexp = "DRAFT|ACTIVE|ARCHIVED", message = "计划状态取值不正确")
            String status
    ) {
    }

    public record Day(
            @NotNull(message = "请选择计划周")
            @Min(value = 1, message = "计划周从 1 开始")
            Integer weekNo,

            @NotNull(message = "请选择训练日")
            @Min(value = 1, message = "训练日取值不正确")
            @Max(value = 7, message = "训练日取值不正确")
            Integer dayOfWeek,

            @NotBlank(message = "请输入训练日名称")
            @Size(max = 50, message = "训练日名称不能超过 50 个字符")
            String title,

            Integer sortNo
    ) {
    }

    public record Exercise(
            @NotNull(message = "请选择动作")
            Long exerciseId,

            Integer sortNo,

            @NotNull(message = "请输入计划组数")
            @Min(value = 1, message = "计划组数至少为 1")
            Integer targetSets,

            @Min(value = 0, message = "目标次数不能为负数")
            Integer targetRepsMin,

            @Min(value = 0, message = "目标次数不能为负数")
            Integer targetRepsMax,

            @DecimalMin(value = "0", message = "目标重量不能为负数")
            BigDecimal targetWeightKg,

            @Min(value = 0, message = "目标时长不能为负数")
            Integer targetDurationSec,

            @Min(value = 0, message = "休息时间不能为负数")
            Integer restSeconds,

            @Pattern(regexp = "NONE|LINEAR|DOUBLE", message = "渐进方式取值不正确")
            String progressionType,

            @DecimalMin(value = "0", message = "递增重量不能为负数")
            BigDecimal incrementKg,

            @Min(value = 0, message = "递增次数不能为负数")
            Integer incrementReps,

            @Size(max = 255, message = "动作备注不能超过 255 个字符")
            String note
    ) {
    }
}
