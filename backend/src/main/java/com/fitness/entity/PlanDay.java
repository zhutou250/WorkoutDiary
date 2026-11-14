package com.fitness.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("plan_day")
public class PlanDay {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long planId;

    private Integer weekNo;

    private Integer dayOfWeek;

    private String title;

    private Integer sortNo;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
