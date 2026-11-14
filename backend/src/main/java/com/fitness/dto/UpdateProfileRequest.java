package com.fitness.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record UpdateProfileRequest(
        @Size(max = 10, message = "性别取值不正确") String gender,
        LocalDate birthDate,

        @DecimalMin(value = "50", message = "身高需在 50—250 厘米之间")
        @DecimalMax(value = "250", message = "身高需在 50—250 厘米之间")
        BigDecimal heightCm,

        @Size(max = 30, message = "训练目标取值不正确") String goal,

        @DecimalMin(value = "0", message = "训练年限不能为负数")
        @DecimalMax(value = "60", message = "训练年限不能超过 60 年")
        BigDecimal trainingYears,

        @Min(value = 1, message = "每周目标至少为 1 次")
        @Max(value = 14, message = "每周目标不能超过 14 次")
        Integer weeklyTarget,

        @Size(max = 5, message = "单位取值不正确") String preferredUnit
) {
}
