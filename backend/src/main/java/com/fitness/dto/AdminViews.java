package com.fitness.dto;

import java.time.LocalDateTime;

public final class AdminViews {

    private AdminViews() {
    }

    public record User(
            Long id,
            String username,
            String nickname,
            String role,
            String status,
            LocalDateTime lastLoginAt,
            LocalDateTime createdAt
    ) {
    }

    public record Platform(
            long userCount,
            long exerciseCount,
            long planCount,
            long sessionCount,
            long adviceCount
    ) {
    }
}
