package com.fitness.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("workout_plan")
public class WorkoutPlan {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String name;

    private String goal;

    private LocalDate startDate;

    private Integer weeks;

    private String status;

    private Boolean isTemplate;

    private Long sourceTemplateId;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
