package com.fitness.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public final class AdminRequests {

    private AdminRequests() {
    }

    public record ExerciseSave(
            @NotBlank(message = "请输入动作名称")
            @Size(max = 100, message = "动作名称不能超过 100 个字符")
            String name,

            @NotBlank(message = "请选择身体部位")
            @Size(max = 30, message = "身体部位取值不正确")
            String bodyPart,

            Long equipmentId,

            @NotBlank(message = "请选择动作类型")
            @Pattern(regexp = "STRENGTH|CARDIO|MOBILITY", message = "动作类型取值不正确")
            String category,

            @NotBlank(message = "请选择记录方式")
            @Pattern(regexp = "REPS|TIME|CARDIO", message = "记录方式取值不正确")
            String mode,

            String description,

            List<String> instructions,

            @Size(max = 255, message = "媒体地址不能超过 255 个字符")
            String mediaUrl
    ) {
    }

    public record UserStatus(
            @NotBlank(message = "请选择状态")
            @Pattern(regexp = "ACTIVE|DISABLED", message = "状态取值不正确")
            String status
    ) {
    }

    public record Feedback(
            Integer rating,
            Boolean helpful,
            @Size(max = 255, message = "评价内容不能超过 255 个字符")
            String comment
    ) {
    }
}
