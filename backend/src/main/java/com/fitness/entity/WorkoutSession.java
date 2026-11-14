package com.fitness.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("workout_session")
public class WorkoutSession {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long planDayId;

    private String planNameSnapshot;

    private String planDayTitleSnapshot;

    private LocalDate sessionDate;

    private LocalDateTime startedAt;

    private LocalDateTime endedAt;

    private String status;

    private BigDecimal overallRpe;

    private String feeling;

    private String note;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
