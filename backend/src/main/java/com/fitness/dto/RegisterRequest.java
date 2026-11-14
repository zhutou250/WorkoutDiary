package com.fitness.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "请输入用户名")
        @Size(min = 4, max = 20, message = "用户名长度需为 4—20 个字符")
        @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "用户名只能包含字母、数字和下划线")
        String username,

        @NotBlank(message = "请输入密码")
        @Size(min = 6, max = 32, message = "密码长度需为 6—32 个字符")
        String password,

        @NotBlank(message = "请输入昵称")
        @Size(max = 20, message = "昵称不能超过 20 个字符")
        String nickname
) {
}
