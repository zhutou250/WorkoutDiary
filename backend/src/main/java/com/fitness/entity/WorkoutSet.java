package com.fitness.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("workout_set")
public class WorkoutSet {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long sessionExerciseId;

    private Integer setNo;

    private String phase;

    private String setType;

    private String side;

    private BigDecimal weightKg;

    private Integer reps;

    private Integer durationSec;

    private BigDecimal distanceM;

    private Integer rir;

    private BigDecimal rpe;

    private Boolean isFailure;

    private Boolean isCompleted;

    private LocalDateTime completedAt;

    private String detailJson;

    private String note;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
