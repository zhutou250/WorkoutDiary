package com.fitness.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("body_metric")
public class BodyMetric {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private LocalDate recordDate;

    private BigDecimal weightKg;

    private BigDecimal bodyFatPct;

    private BigDecimal waistCm;

    private String note;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
