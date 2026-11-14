package com.fitness.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public final class BodyMetricRequests {

    private BodyMetricRequests() {
    }

    public record Upsert(
            @NotNull(message = "请选择记录日期")
            LocalDate recordDate,

            @DecimalMin(value = "0", message = "体重不能小于 0")
            @DecimalMax(value = "500", message = "体重不能超过 500 kg")
            BigDecimal weightKg,

            @DecimalMin(value = "0", message = "体脂率不能小于 0")
            @DecimalMax(value = "100", message = "体脂率不能大于 100")
            BigDecimal bodyFatPct,

            @DecimalMin(value = "0", message = "腰围不能小于 0")
            @DecimalMax(value = "300", message = "腰围不能超过 300 cm")
            BigDecimal waistCm,

            @Size(max = 255, message = "备注不能超过 255 个字符")
            String note
    ) {
    }
}
