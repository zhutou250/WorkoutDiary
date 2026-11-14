package com.fitness.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("user_profile")
public class UserProfile {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String gender;

    private LocalDate birthDate;

    private BigDecimal heightCm;

    private String goal;

    private BigDecimal trainingYears;

    private Integer weeklyTarget;

    private String preferredUnit;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
