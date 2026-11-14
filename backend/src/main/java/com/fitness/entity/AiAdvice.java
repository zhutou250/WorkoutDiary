package com.fitness.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("ai_advice")
public class AiAdvice {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private LocalDate periodStart;

    private LocalDate periodEnd;

    private String status;

    private String modelName;

    private String promptVersion;

    private String requestDigest;

    private String responseText;

    private Boolean fallbackUsed;

    private String errorMessage;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
