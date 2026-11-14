package com.fitness.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("exercise_muscle")
public class ExerciseMuscle {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long exerciseId;

    private Long muscleGroupId;

    private String role;

    private BigDecimal weight;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
