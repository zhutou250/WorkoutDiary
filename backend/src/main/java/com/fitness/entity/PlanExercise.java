package com.fitness.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("plan_exercise")
public class PlanExercise {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long planDayId;

    private Long exerciseId;

    private Integer sortNo;

    private Integer targetSets;

    private Integer targetRepsMin;

    private Integer targetRepsMax;

    private BigDecimal targetWeightKg;

    private Integer targetDurationSec;

    private Integer restSeconds;

    private String progressionType;

    private BigDecimal incrementKg;

    private Integer incrementReps;

    private String note;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
