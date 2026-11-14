package com.fitness.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("session_exercise")
public class SessionExercise {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long sessionId;

    private Long exerciseId;

    private Long planExerciseId;

    private String exerciseNameSnapshot;

    private Integer sortNo;

    private Integer plannedSets;

    private Integer plannedRepsMin;

    private Integer plannedRepsMax;

    private BigDecimal plannedWeightKg;

    private Integer plannedDurationSec;

    private String deviationType;

    private String actualNote;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
